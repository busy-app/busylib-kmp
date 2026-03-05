package net.flipper.bridge.device.firmwareupdate.updater.api

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.job
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.device.firmwareupdate.downloader.api.FirmwareDownloaderApi
import net.flipper.bridge.device.firmwareupdate.downloader.api.FirmwareDownloaderApiImpl
import net.flipper.bridge.device.firmwareupdate.updater.diff.FwUpdateStateDiff
import net.flipper.bridge.device.firmwareupdate.updater.mapper.FwUpdateStatusMapper
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.bridge.device.firmwareupdate.uploader.api.FirmwareUploaderApi
import net.flipper.bridge.device.firmwareupdate.uploader.api.FirmwareUploaderApiImpl
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.asFlow
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.cancelPrevious
import net.flipper.core.busylib.ktx.common.orNullable
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.ktor.di.qualifier.KtorNetworkClientQualifier
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@ContributesBinding(BusyLibGraph::class, FirmwareUpdaterApi::class)
@SingleIn(BusyLibGraph::class)
class FirmwareUpdaterApiImpl(
    private val fFeatureProvider: FFeatureProvider,
    private val scope: CoroutineScope,
    @KtorNetworkClientQualifier
    private val httpClient: HttpClient,
    private val previousVersionFlowProvider: PreviousVersionFlowProvider,
) : FirmwareUpdaterApi, LogTagProvider by TaggedLogger("UpdaterApi") {
    private val firmwareDownloaderApi: FirmwareDownloaderApi = FirmwareDownloaderApiImpl(
        httpClient = httpClient
    )
    private val firmwareUploaderApi: FirmwareUploaderApi = FirmwareUploaderApiImpl(
        fFeatureProvider = fFeatureProvider,
    )

    private val lanUpdaterScope = scope.asSingleJobScope()

    override val state: WrappedStateFlow<FwUpdateState> = combine(
        flow = fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
            .map { status -> status.tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>() }
            .map { status -> status?.featureApi }
            .flatMapLatest { feature -> feature?.updateStatusFlow.orNullable() }
            .shareIn(scope, SharingStarted.WhileSubscribed(), 1),
        flow2 = fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
            .map { status -> status.tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>() }
            .map { status -> status?.featureApi }
            .flatMapLatest { feature -> feature?.updateVersionFlow.orNullable() }
            .filterNotNull()
            .shareIn(scope, SharingStarted.WhileSubscribed(), 1),
        flow3 = firmwareDownloaderApi.state,
        flow4 = firmwareUploaderApi.state,
        transform = { updateStatus, bsbUpdateVersion, downloaderState, uploaderState ->
            when (bsbUpdateVersion) {
                is BsbUpdateVersion.Default -> {
                    FwUpdateStatusMapper.toFwUpdateState(
                        updateStatus = updateStatus,
                    )
                }

                is BsbUpdateVersion.Url -> {
                    FwUpdateStatusMapper.toFwUpdateState(
                        downloaderState = downloaderState,
                        uploaderState = uploaderState,
                        bsbUrlUpdateVersion = bsbUpdateVersion,
                    )
                }
            }.also { info { "#state: $it" } }
        }
    ).stateIn(scope, SharingStarted.Eagerly, FwUpdateState.Pending).wrap()

    override val events = previousVersionFlowProvider
        .getAutoRestartedPreviousVersionFlow(state)
        .combine(state) { versionsModel, fwUpdateState ->
            FwUpdateStateDiff.compareAndGetEvent(
                fwUpdateState = fwUpdateState,
                busyBarVersionTransition = versionsModel
            )
        }
        .distinctUntilChanged()
        .onEach { info { "#event: $it" } }
        .filterNotNull()
        .shareIn(scope, SharingStarted.Eagerly)
        .asFlow()
        .wrap()

    override suspend fun stopFirmwareUpdate(): CResult<Unit> {
        val currentUpdateVersion = fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
            .map { status -> status.tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>() }
            .map { status -> status?.featureApi }
            .flatMapLatest { feature -> feature?.updateVersionFlow.orNullable() }
            .filterNotNull()
            .firstOrNull()
        return when (currentUpdateVersion) {
            is BsbUpdateVersion.Default -> {
                fFeatureProvider.get<FRpcFeatureApi>()
                    .filterIsInstance<FFeatureStatus.Supported<FRpcFeatureApi>>()
                    .first()
                    .featureApi
                    .fRpcUpdaterApi
                    .startUpdateAbortDownload()
                    .map { }
                    .toCResult()
            }

            is BsbUpdateVersion.Url -> {
                lanUpdaterScope.cancelPrevious().join()
                CResult.success(Unit)
            }

            null -> CResult.success(Unit)
        }
    }

    private suspend fun awaitDeviceReconnected() {
        info { "#startUpdateInstall upload finished! Awaiting null version" }
        fFeatureProvider.get<FDeviceInfoFeatureApi>()
            .map { status -> status.tryCast<FFeatureStatus.Supported<FDeviceInfoFeatureApi>>() }
            .map { status -> status?.featureApi }
            .flatMapLatest { feature -> feature?.deviceVersionFlow.orNullable() }
            .filter { version -> version == null }
            .first()
        info { "#startUpdateInstall awaiting for version!" }
        fFeatureProvider.get<FDeviceInfoFeatureApi>()
            .map { status -> status.tryCast<FFeatureStatus.Supported<FDeviceInfoFeatureApi>>() }
            .map { status -> status?.featureApi }
            .flatMapLatest { feature -> feature?.deviceVersionFlow.orNullable() }
            .filterNotNull()
            .first()
        info { "#startUpdateInstall device connected" }
    }

    /**
     * Starts an update install depending on current [BsbUpdateVersion]
     * Using map for flow here in case connection type changed from lan to other type
     * @see BsbUpdateVersion
     */
    override suspend fun startUpdateInstall(): CResult<Unit> {
        info { "#startUpdateInstall" }
        return lanUpdaterScope.async {
            coroutineContext.job.invokeOnCompletion {
                firmwareDownloaderApi.reset()
                firmwareUploaderApi.reset()
            }
            fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
                .map { status -> status.tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>() }
                .flatMapLatest { status -> status?.featureApi?.updateVersionFlow.orNullable() }
                .filterNotNull()
                .mapLatest { bsbUpdateVersion ->
                    firmwareDownloaderApi.reset()
                    firmwareUploaderApi.reset()
                    info { "#startUpdateInstall bsbUpdateVersion: $bsbUpdateVersion" }
                    when (bsbUpdateVersion) {
                        is BsbUpdateVersion.Default -> {
                            fFeatureProvider.get<FRpcFeatureApi>()
                                .filterIsInstance<FFeatureStatus.Supported<FRpcFeatureApi>>()
                                .first()
                                .featureApi
                                .fRpcUpdaterApi
                                .startUpdateInstall(bsbUpdateVersion.version)
                                .map { }
                        }

                        is BsbUpdateVersion.Url -> {
                            firmwareDownloaderApi.download(bsbUpdateVersion)
                                .onSuccess { info { "#startUpdateInstall download finished" } }
                                .onFailure { t -> error(t) { "#startUpdateInstall could not download" } }
                                .mapCatching { path ->
                                    info { "#startUpdateInstall start uploading" }
                                    firmwareUploaderApi
                                        .uploadAndInstall(path) { firmwareDownloaderApi.reset() }
                                        .onFailure { t -> error(t) { "#startUpdateInstall could not upload" } }
                                        .getOrThrow()
                                }
                        }
                    }
                }
                .map { result -> result.toCResult() }
                .first()
                .also { awaitDeviceReconnected() }
        }.await()
    }
}
