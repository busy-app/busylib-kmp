package net.flipper.bridge.device.firmwareupdate.updater.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.feature.provider.api.getSync
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarVersion
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.bridge.connection.transport.common.api.serial.hasCapability
import net.flipper.bridge.device.firmwareupdate.downloader.api.FirmwareDownloaderApi
import net.flipper.bridge.device.firmwareupdate.updater.diff.FwUpdateStateDiff
import net.flipper.bridge.device.firmwareupdate.updater.mapper.FwUpdateStatusMapper
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.bridge.device.firmwareupdate.updater.provider.AvailableVersionChangelogProvider
import net.flipper.bridge.device.firmwareupdate.uploader.api.FirmwareUploaderApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.cancelPrevious
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.mapCached
import net.flipper.core.busylib.ktx.common.orNullable
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.map

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, FirmwareUpdaterApi::class)
class FirmwareUpdaterApiImpl(
    private val fFeatureProvider: FFeatureProvider,
    private val scope: CoroutineScope,
    private val availableVersionChangelogProvider: AvailableVersionChangelogProvider,
    private val firmwareDownloaderApi: FirmwareDownloaderApi,
    private val firmwareUploaderApi: FirmwareUploaderApi,
    private val fDeviceOrchestrator: FDeviceOrchestrator
) : FirmwareUpdaterApi, LogTagProvider by TaggedLogger("UpdaterApi") {
    private val lanUpdaterScope = scope.asSingleJobScope()
    private val changelogSharedFlow = availableVersionChangelogProvider
        .getLatestAvailableChangelogFlow()
        .shareIn(scope, SharingStarted.Eagerly, 1)

    override val state: WrappedStateFlow<FwUpdateState> = combine(
        flow = fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
            .map { status ->
                status
                    .tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>()
                    ?.featureApi
            }
            .flatMapLatest { feature -> feature?.updateStatusFlow.orNullable() },
        flow2 = changelogSharedFlow,
        flow3 = firmwareDownloaderApi.state,
        flow4 = firmwareUploaderApi.state,
        transform = { updateStatus, changelogOrNull, downloaderState, uploaderState ->
            FwUpdateStatusMapper.toFwUpdateState(
                updateStatus = updateStatus,
                changelogOrNull = changelogOrNull,
                downloaderState = downloaderState,
                uploaderState = uploaderState
            )
        }
    )
        .mapCached { currentFwUpdateState, previousFwUpdateState: FwUpdateState? ->
            FwUpdateStateDiff.combineDiff(
                previous = previousFwUpdateState,
                latest = currentFwUpdateState,
                getCurrentVersion = {
                    exponentialRetry {
                        fFeatureProvider.getSync<FRpcFeatureApi>()
                            ?.fRpcSystemApi
                            ?.getVersion()
                            ?.map(BusyBarVersion::version)
                            ?: error("Could not get FRpcFeatureApi")
                    }
                }
            )
        }
        .onEach { info { "#state FwUpdateStateDiff: $it" } }
        .stateIn(scope, SharingStarted.Eagerly, FwUpdateState.Pending)
        .wrap()

    override suspend fun stopFirmwareUpdate(): CResult<Unit> {
        lanUpdaterScope.cancelPrevious()
        return fFeatureProvider.get<FRpcFeatureApi>()
            .filterIsInstance<FFeatureStatus.Supported<FRpcFeatureApi>>()
            .first()
            .featureApi
            .fRpcUpdaterApi
            .startUpdateAbortDownload()
            .map { }
            .toCResult()
    }

    override suspend fun startVersionInstall(version: String): CResult<Unit> {
        val deviceApi = fDeviceOrchestrator.getState()
            .first()
            .tryCast<FDeviceConnectStatus.Connected>()
            ?.deviceApi
            ?.tryCast<FHTTPDeviceApi>()
            ?: return CResult.failure(IllegalStateException("Device is not connected"))

        val canDownloadUpdate = deviceApi
            .hasCapability(FHTTPTransportCapability.BB_DOWNLOAD_UPDATE_SUPPORTED)
            .first()

        info { "#startVersionInstall $canDownloadUpdate ${deviceApi.getCapabilities().value}" }
        return CResult.failure(IllegalStateException(""))
        if (canDownloadUpdate) {
            lanUpdaterScope.launch(SingleJobMode.CANCEL_PREVIOUS) {
                firmwareDownloaderApi.downloadAndUpload(version)
            }
            return CResult.success(Unit)
        } else {
            return fFeatureProvider.get<FRpcFeatureApi>()
                .filterIsInstance<FFeatureStatus.Supported<FRpcFeatureApi>>()
                .first()
                .featureApi
                .fRpcUpdaterApi
                .startUpdateInstall(version)
                .map { }
                .toCResult()
        }
    }
}
