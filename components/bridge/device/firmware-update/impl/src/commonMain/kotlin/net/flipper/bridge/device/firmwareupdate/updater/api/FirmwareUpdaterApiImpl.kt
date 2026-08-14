package net.flipper.bridge.device.firmwareupdate.updater.api

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.job
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.feature.provider.api.getSync
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.BsbRpcError
import net.flipper.bridge.connection.feature.rpc.api.model.ErrorResponse
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.bridge.device.firmwareupdate.downloader.api.FirmwareDownloaderApi
import net.flipper.bridge.device.firmwareupdate.downloader.api.FirmwareDownloaderApiImpl
import net.flipper.bridge.device.firmwareupdate.status.api.UpdateStatusProvider
import net.flipper.bridge.device.firmwareupdate.updater.log.FwUpdateStateLogger
import net.flipper.bridge.device.firmwareupdate.updater.mapper.FwUpdateStatusMapper
import net.flipper.bridge.device.firmwareupdate.updater.model.FwInstallRequest
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateEvent
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.bridge.device.firmwareupdate.updater.model.StartUpdateResponse
import net.flipper.bridge.device.firmwareupdate.uploader.api.FirmwareUploaderApi
import net.flipper.bridge.device.firmwareupdate.uploader.api.FirmwareUploaderApiImpl
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.busylib.kmp.components.core.buildkonfig.BuildKonfig
import net.flipper.core.busylib.ktx.common.asFlow
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.cancelPrevious
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.orNullable
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.ktor.di.qualifier.KtorNetworkClientQualifier
import kotlin.coroutines.coroutineContext
import kotlin.time.Instant

@Inject
@ContributesBinding(BusyLibGraph::class, binding<FirmwareUpdaterApi>())
@SingleIn(BusyLibGraph::class)
class FirmwareUpdaterApiImpl(
    private val fFeatureProvider: FFeatureProvider,
    @KtorNetworkClientQualifier
    private val httpClient: HttpClient,
    private val previousVersionFlowProvider: PreviousVersionFlowProvider,
    private val scope: CoroutineScope,
    private val updateStatusProvider: UpdateStatusProvider,
    private val fDevicePersistedStorage: FDevicePersistedStorage
) : FirmwareUpdaterApi, LogTagProvider by TaggedLogger("UpdaterApi") {
    private val firmwareDownloaderApi: FirmwareDownloaderApi = FirmwareDownloaderApiImpl(
        httpClient = httpClient
    )
    private val firmwareUploaderApi: FirmwareUploaderApi = FirmwareUploaderApiImpl(
        fFeatureProvider = fFeatureProvider,
    )

    private val lanUpdaterScope = scope.asSingleJobScope()

    private val installRequestFlow = MutableStateFlow(FwInstallRequest.NONE)

    private val stateLogger by lazy { FwUpdateStateLogger() }

    override val state: WrappedStateFlow<FwUpdateState> = combine(
        flow = updateStatusProvider.getUpdateStatus(),
        flow2 = fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
            .map { status -> status.tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>() }
            .map { status -> status?.featureApi }
            .flatMapLatest { feature -> feature?.updateVersionFlow.orNullable() }
            .distinctUntilChanged(),
        flow3 = firmwareDownloaderApi.state,
        flow4 = firmwareUploaderApi.state,
        flow5 = installRequestFlow,
        transform = { updateStatusSource, bsbUpdateVersion, downloaderState, uploaderState, installRequest ->
            val result = FwUpdateStatusMapper.map(
                updateStatusSource,
                bsbUpdateVersion,
                downloaderState,
                uploaderState,
                installRequest
            )
            if (BuildKonfig.IS_VERBOSE_LOG_ENABLED && BuildKonfig.IS_LOG_ENABLED) {
                stateLogger.logIfChanged(
                    result = result,
                    updateStatusSource = updateStatusSource,
                    bsbUpdateVersion = bsbUpdateVersion,
                    downloaderState = downloaderState,
                    uploaderState = uploaderState,
                    installRequest = installRequest
                )
            }
            return@combine result
        }
    ).stateIn(scope, SharingStarted.Lazily, FwUpdateState.Pending).wrap()

    override val events = previousVersionFlowProvider
        .getPreviousVersionFlow(state)
        .map { versionsModel ->
            when {
                versionsModel == null -> null
                versionsModel.previousVersion == null -> null
                versionsModel.currentVersion == versionsModel.previousVersion -> {
                    FwUpdateEvent.UpdateFailed
                }

                versionsModel.currentVersion != versionsModel.previousVersion -> {
                    FwUpdateEvent.UpdateFinished
                }

                else -> null
            }
        }
        .filterNotNull()
        .shareIn(scope, SharingStarted.Lazily)
        .asFlow()
        .wrap()

    override suspend fun stopFirmwareUpdate(): CResult<Unit> {
        lanUpdaterScope.cancelPrevious().join()
        val currentUpdateVersion = fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
            .map { status -> status.tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>() }
            .map { status -> status?.featureApi }
            .flatMapLatest { feature -> feature?.updateVersionFlow.orNullable() }
            .filterNotNull()
            .firstOrNull()
        return when (currentUpdateVersion) {
            is BsbUpdateVersion.ReadyToUpdate.Default -> {
                fFeatureProvider.get<FRpcFeatureApi>()
                    .filterIsInstance<FFeatureStatus.Supported<*>>()
                    .filter { fFeatureStatus -> fFeatureStatus.featureApi is FRpcFeatureApi }
                    .filterIsInstance<FFeatureStatus.Supported<FRpcFeatureApi>>()
                    .first()
                    .featureApi
                    .fRpcUpdaterApi
                    .startUpdateAbortDownload()
                    .map { }
                    .toCResult()
            }

            is BsbUpdateVersion.ReadyToUpdate.Url -> CResult.success(Unit)

            else -> CResult.success(Unit)
        }
    }

    /**
     * With different FWUpdates, we have different behavior
     * of how BSB reacts to our requests
     * The most stable solution is to see the difference between it's boot times
     */
    private suspend fun getBootTimeFlow(): Flow<Instant> {
        return fFeatureProvider.get<FDeviceInfoFeatureApi>()
            .map { status -> status.tryCast<FFeatureStatus.Supported<FDeviceInfoFeatureApi>>() }
            .mapNotNull { status -> status?.featureApi }
            .map { featureApi ->
                exponentialRetry { featureApi.getDeviceInfo().toKotlinResult() }
            }
            .map { statusSystem -> statusSystem.bootTime }
            .filterNotNull()
    }

    private suspend fun awaitDeviceReconnected(previousBootTime: Instant) {
        info { "#startUpdateInstall upload finished! Awaiting new boot time" }
        getBootTimeFlow().filter { instant -> instant > previousBootTime }.first()
        info { "#startUpdateInstall device connected!" }
    }

    private fun getUpdateVersionFlow(): Flow<BsbUpdateVersion?> {
        return fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
            .map { status -> status.tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>() }
            .flatMapLatest { status -> status?.featureApi?.updateVersionFlow.orNullable() }
    }

    private suspend fun awaitUpdateInstalled(installedVersion: String) {
        info { "#startUpdateInstall awaiting $installedVersion to stop being offered" }
        getUpdateVersionFlow().first { bsbUpdateVersion ->
            when (bsbUpdateVersion) {
                BsbUpdateVersion.NoUpdateAvailable -> true
                is BsbUpdateVersion.ReadyToUpdate -> bsbUpdateVersion.version != installedVersion
                BsbUpdateVersion.CheckingOnBBInProgress,
                BsbUpdateVersion.FailedToCheck,
                BsbUpdateVersion.Loading,
                null -> false
            }
        }
        info { "#startUpdateInstall $installedVersion is installed" }
    }

    private suspend fun startUpdateInstallInternal(
        bsbUpdateVersion: BsbUpdateVersion.ReadyToUpdate
    ): StartUpdateResponse {
        return when (bsbUpdateVersion) {
            is BsbUpdateVersion.ReadyToUpdate.Default -> {
                fFeatureProvider.get<FRpcFeatureApi>()
                    .filterIsInstance<FFeatureStatus.Supported<*>>()
                    .filter { fFeatureStatus -> fFeatureStatus.featureApi is FRpcFeatureApi }
                    .filterIsInstance<FFeatureStatus.Supported<FRpcFeatureApi>>()
                    .first()
                    .featureApi
                    .fRpcUpdaterApi
                    .startUpdateInstall(bsbUpdateVersion.version)
                    .fold(
                        onSuccess = { apiResponse ->
                            when (apiResponse) {
                                is ErrorResponse if apiResponse.error == BsbRpcError.BATTERY_LOW.error -> {
                                    StartUpdateResponse.BatteryLow
                                }

                                is ErrorResponse -> {
                                    StartUpdateResponse.Failure(Throwable(apiResponse.error))
                                }

                                is SuccessResponse -> StartUpdateResponse.Success
                            }
                        },
                        onFailure = { t ->
                            StartUpdateResponse.Failure(t)
                        }
                    )
            }

            is BsbUpdateVersion.ReadyToUpdate.Url -> {
                firmwareDownloaderApi.download(bsbUpdateVersion)
                    .onSuccess { info { "#startUpdateInstall download finished" } }
                    .onFailure { t -> error(t) { "#startUpdateInstall could not download" } }
                    .map { path ->
                        info { "#startUpdateInstall start uploading" }
                        firmwareUploaderApi.uploadAndInstall(path) { firmwareDownloaderApi.reset() }
                    }
                    .fold(
                        onSuccess = { startUpdateResponse ->
                            startUpdateResponse
                        },
                        onFailure = { t ->
                            StartUpdateResponse.Failure(t)
                        }
                    )
            }
        }
    }

    private suspend fun startDeviceInstall(
        bsbUpdateVersion: BsbUpdateVersion.ReadyToUpdate.Default
    ): StartUpdateResponse {
        val startUpdateResponse = startUpdateInstallInternal(bsbUpdateVersion)
        when (startUpdateResponse) {
            StartUpdateResponse.BatteryLow,
            is StartUpdateResponse.Failure -> Unit

            StartUpdateResponse.Success -> {
                installRequestFlow.value = FwInstallRequest.STARTED
                awaitUpdateInstalled(bsbUpdateVersion.version)
            }
        }
        return startUpdateResponse
    }

    private suspend fun startLanInstall(
        bsbUpdateVersion: BsbUpdateVersion.ReadyToUpdate.Url
    ): StartUpdateResponse {
        val bootTimeScope = coroutineContext.minusKey(Job)
            .plus(SupervisorJob(coroutineContext.job))
            .let(::CoroutineScope)
        val bootTimeFlow = getBootTimeFlow().shareIn(bootTimeScope, SharingStarted.Eagerly, 1)
        val startUpdateResponse = startUpdateInstallInternal(bsbUpdateVersion)
        when (startUpdateResponse) {
            StartUpdateResponse.BatteryLow,
            is StartUpdateResponse.Failure -> bootTimeScope.cancel()

            StartUpdateResponse.Success -> {
                installRequestFlow.value = FwInstallRequest.STARTED
                info { "#startUpdateInstall bootTimeFlow wait" }
                val bootTime = bootTimeFlow.first()
                info { "#startUpdateInstall bootTimeFlow cancel" }
                bootTimeScope.cancel()
                awaitDeviceReconnected(bootTime)
            }
        }
        return startUpdateResponse
    }

    /**
     * Starts an update install depending on current [BsbUpdateVersion]
     * Using map for flow here in case connection type changed from lan to other type
     * @see BsbUpdateVersion
     */
    override suspend fun startUpdateInstall(): StartUpdateResponse {
        return lanUpdaterScope.async {
            installRequestFlow.value = FwInstallRequest.REQUESTED
            coroutineContext.job.invokeOnCompletion {
                installRequestFlow.value = FwInstallRequest.NONE
                firmwareDownloaderApi.reset()
                firmwareUploaderApi.reset()
            }
            val bsbUpdateVersion = getUpdateVersionFlow()
                .filterIsInstance<BsbUpdateVersion.ReadyToUpdate>()
                .first()

            info { "#startUpdateInstall bsbUpdateVersion: $bsbUpdateVersion" }
            firmwareDownloaderApi.reset()
            firmwareUploaderApi.reset()
            when (bsbUpdateVersion) {
                is BsbUpdateVersion.ReadyToUpdate.Default -> {
                    startDeviceInstall(bsbUpdateVersion)
                }

                is BsbUpdateVersion.ReadyToUpdate.Url -> {
                    startLanInstall(bsbUpdateVersion)
                }
            }
        }.await()
    }

    override suspend fun checkForUpdates(): CResult<Unit> {
        return fFeatureProvider.getSync<FRpcFeatureApi>()
            ?.fRpcUpdaterApi
            ?.startUpdateCheck()
            ?.map { }
            ?.toCResult()
            ?: CResult.failure(IllegalStateException("RPC feature is null"))
    }

    // Reset updater when connected to another device
    init {
        fDevicePersistedStorage.getCurrentDeviceFlow()
            .map { bUSYBar -> bUSYBar?.uniqueId }
            .distinctUntilChanged()
            .onEach {
                lanUpdaterScope.cancelPrevious().join()
                firmwareDownloaderApi.reset()
                firmwareUploaderApi.reset()
            }
            .launchIn(scope)
    }
}
