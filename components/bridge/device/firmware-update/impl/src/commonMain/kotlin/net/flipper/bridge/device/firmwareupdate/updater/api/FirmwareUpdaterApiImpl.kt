package net.flipper.bridge.device.firmwareupdate.updater.api

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarVersion
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
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
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.cancelPrevious
import net.flipper.core.busylib.ktx.common.flatMapCached
import net.flipper.core.busylib.ktx.common.merge
import net.flipper.core.busylib.ktx.common.orNullable
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.ktor.di.qualifier.KtorNetworkClientQualifier
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(BusyLibGraph::class, FirmwareUpdaterApi::class)
class FirmwareUpdaterApiImpl(
    private val fFeatureProvider: FFeatureProvider,
    private val scope: CoroutineScope,
    private val fDeviceOrchestrator: FDeviceOrchestrator,
    @KtorNetworkClientQualifier
    private val httpClient: HttpClient,
) : FirmwareUpdaterApi, LogTagProvider by TaggedLogger("UpdaterApi") {
    private val firmwareDownloaderApi: FirmwareDownloaderApi = FirmwareDownloaderApiImpl(
        httpClient = httpClient
    )
    private val firmwareUploaderApi: FirmwareUploaderApi = FirmwareUploaderApiImpl(
        fFeatureProvider = fFeatureProvider,
        fDeviceOrchestrator = fDeviceOrchestrator
    )

    private val lanUpdaterScope = scope.asSingleJobScope()
    private val previousVersionsFlow = flow {
        var previousVersionOrNull: BusyBarVersion? = null
        var currentVersionOrNull: BusyBarVersion? = null
        fFeatureProvider.get<FDeviceInfoFeatureApi>()
            .filterIsInstance<FFeatureStatus.Supported<FDeviceInfoFeatureApi>>()
            .flatMapLatest { status -> status.featureApi.deviceVersionFlow }
            .onEach { newCurrentVersion ->
                when {
                    currentVersionOrNull == null -> {
                        previousVersionOrNull = newCurrentVersion
                        currentVersionOrNull = newCurrentVersion
                    }

                    currentVersionOrNull == newCurrentVersion -> {
                        // nothing
                    }

                    currentVersionOrNull != newCurrentVersion -> {
                        previousVersionOrNull = currentVersionOrNull
                        currentVersionOrNull = newCurrentVersion
                    }
                }
                emit(previousVersionOrNull)
            }
            .collect()
    }
        .onEach { info { "#previousVersionsFlow: $it" } }
        .merge(flowOf(null))
        .shareIn(scope, SharingStarted.Lazily, 1)

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
            .map { bsbUpdateVersion -> bsbUpdateVersion?.tryCast<BsbUpdateVersion.Url>() }
            .shareIn(scope, SharingStarted.WhileSubscribed(), 1),
        flow3 = firmwareDownloaderApi.state,
        flow4 = firmwareUploaderApi.state,
        transform = { updateStatus, bsbUrlUpdateVersion, downloaderState, uploaderState ->
            FwUpdateStatusMapper.toFwUpdateState(
                updateStatus = updateStatus,
                downloaderState = downloaderState,
                uploaderState = uploaderState,
                bsbUrlUpdateVersion = bsbUrlUpdateVersion
            )
        }
    )
        .onEach { info { "#state FwUpdateStatusMapper: $it" } }
        .flatMapCached { currentFwUpdateState, previousFwUpdateState: FwUpdateState? ->
            fFeatureProvider.get<FDeviceInfoFeatureApi>()
                .filterIsInstance<FFeatureStatus.Supported<FDeviceInfoFeatureApi>>()
                .first()
                .featureApi
                .deviceVersionFlow
                .map { currentVersion ->
                    FwUpdateStateDiff.combineDiff(
                        previous = previousFwUpdateState,
                        latest = currentFwUpdateState,
                        currentVersion = currentVersion,
                        previousVersion = previousVersionsFlow.first()
                    )
                }
        }
        .onEach { info { "#state FwUpdateStateDiff: $it" } }
        .stateIn(scope, SharingStarted.Eagerly, FwUpdateState.Pending)
        .wrap()

    override suspend fun stopFirmwareUpdate(): CResult<Unit> {
        val currentUpdateVersion = fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
            .map { status -> status.tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>() }
            .map { status -> status?.featureApi }
            .flatMapLatest { feature -> feature?.updateVersionFlow.orNullable() }
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

    /**
     * Starts an update install depending on current [BsbUpdateVersion]
     * Using map for flow here in case connection type changed from lan to other type
     * @see BsbUpdateVersion
     */
    override suspend fun startUpdateInstall(): CResult<Unit> {
        info { "#startUpdateInstall" }
        return lanUpdaterScope.withJobMode(SingleJobMode.CANCEL_PREVIOUS) {
            coroutineContext.job.invokeOnCompletion {
                firmwareDownloaderApi.reset()
                firmwareUploaderApi.reset()
            }
            fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
                .map { status -> status.tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>() }
                .flatMapLatest { status -> status?.featureApi?.updateVersionFlow.orNullable() }
                .onEach { info { "#startUpdateInstall version status: $it" } }
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
                                        .uploadAndInstall(path)
                                        .onFailure { t -> error(t) { "#startUpdateInstall could not upload" } }
                                        .getOrThrow()
                                }
                                .also { firmwareDownloaderApi.reset() }
                                .also { firmwareUploaderApi.reset() }
                        }
                    }
                }
                .map { result -> result.toCResult() }
                .first()
        }.await()
    }
}
