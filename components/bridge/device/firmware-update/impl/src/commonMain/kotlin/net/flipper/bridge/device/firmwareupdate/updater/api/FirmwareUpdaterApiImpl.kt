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
import kotlinx.coroutines.withTimeoutOrNull
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.feature.provider.api.getSync
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.device.firmwareupdate.downloader.api.FirmwareDownloaderApi
import net.flipper.bridge.device.firmwareupdate.downloader.api.FirmwareDownloaderApiImpl
import net.flipper.bridge.device.firmwareupdate.status.api.UpdateStatusProvider
import net.flipper.bridge.device.firmwareupdate.status.model.UpdateStatusSource
import net.flipper.bridge.device.firmwareupdate.updater.log.FwUpdateStateLogger
import net.flipper.bridge.device.firmwareupdate.updater.mapper.FwUpdateStatusMapper
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateEvent
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.bridge.device.firmwareupdate.uploader.api.FirmwareUploaderApi
import net.flipper.bridge.device.firmwareupdate.uploader.api.FirmwareUploaderApiImpl
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.map
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.busylib.kmp.components.core.buildkonfig.BuildKonfig
import net.flipper.core.busylib.ktx.common.asFlow
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.cancelPrevious
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.mapSuspendCatching
import net.flipper.core.busylib.ktx.common.orNullable
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.ktor.di.qualifier.KtorNetworkClientQualifier
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds
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
    private val defaultUpdaterScope = scope.asSingleJobScope()

    private val installRequestedFlow = MutableStateFlow(false)

    private val updatingDeviceIdFlow = MutableStateFlow<String?>(null)

    override val updatingDeviceId: WrappedStateFlow<String?> = updatingDeviceIdFlow.wrap()

    private val stateLogger by lazy { FwUpdateStateLogger() }

    /**
     * Id of the currently connected device, `null` while disconnected
     */
    private val connectedDeviceIdFlow = updateStatusProvider.getConnectedDeviceId()
        .stateIn(scope, SharingStarted.Eagerly, null)

    override val state: WrappedStateFlow<FwUpdateState> = combine(
        flow = updateStatusProvider.getUpdateStatus(),
        flow2 = updateStatusProvider.getUpdateVersion(),
        flow3 = firmwareDownloaderApi.state,
        flow4 = firmwareUploaderApi.state,
        flow5 = installRequestedFlow,
        transform = { updateStatusSource, deviceUpdateVersion, downloaderState, uploaderState, isInstallRequested ->
            val bsbUpdateVersion = deviceUpdateVersion?.version
            // A status of one device must never be merged with the version of another
            // (switch window: the old device's Cached status can outlive its connection)
            val statusDeviceId = updateStatusSource.status?.deviceId
            val isForeignStatus = statusDeviceId != null &&
                deviceUpdateVersion != null &&
                statusDeviceId != deviceUpdateVersion.deviceId
            val matchedStatusSource = if (isForeignStatus) {
                debug {
                    "#state drop status of $statusDeviceId, " +
                        "connected device is ${deviceUpdateVersion?.deviceId}"
                }
                UpdateStatusSource.Fresh(null)
            } else {
                updateStatusSource
            }
            val result = FwUpdateStatusMapper.map(
                matchedStatusSource,
                bsbUpdateVersion,
                downloaderState,
                uploaderState,
                isInstallRequested
            )
            when (result) {
                is FwUpdateState.UpdateAvailable,
                is FwUpdateState.NoUpdateAvailable,
                is FwUpdateState.LowBattery,
                is FwUpdateState.CheckingVersion,
                is FwUpdateState.CouldNotCheckUpdate,
                is FwUpdateState.DownloadFailure,
                is FwUpdateState.Uploading,
                is FwUpdateState.Updating,
                is FwUpdateState.Downloading -> installRequestedFlow.emit(false)

                is FwUpdateState.Preparing,
                is FwUpdateState.Pending -> Unit
            }
            if (BuildKonfig.IS_VERBOSE_LOG_ENABLED && BuildKonfig.IS_LOG_ENABLED) {
                stateLogger.logIfChanged(
                    result = result,
                    updateStatusSource = matchedStatusSource,
                    bsbUpdateVersion = bsbUpdateVersion,
                    downloaderState = downloaderState,
                    uploaderState = uploaderState,
                    isInstallRequested = isInstallRequested
                )
            }
            return@combine result
        }
    ).stateIn(scope, SharingStarted.Lazily, FwUpdateState.Pending).wrap()

    private val eventsSharedFlow = previousVersionFlowProvider
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

    override val events = eventsSharedFlow
        .asFlow()
        .wrap()

    override suspend fun stopFirmwareUpdate(): CResult<Unit> {
        // Both feature lookups below only resolve while a device is connected. Unbounded,
        // Cancel on a disconnected device suspends indefinitely, and the parked lookup
        // later resumes against whatever device connects NEXT — sending the abort to the
        // wrong device. Cancel is best-effort by contract: when the device is unreachable
        // within the timeout we just release the local tracking
        val currentUpdateVersion = withTimeoutOrNull(STOP_FEATURE_LOOKUP_TIMEOUT) {
            fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
                .map { status -> status.tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>() }
                .map { status -> status?.featureApi }
                .flatMapLatest { feature -> feature?.updateVersionFlow.orNullable() }
                .filterNotNull()
                .firstOrNull()
        }
        return when (currentUpdateVersion?.version) {
            is BsbUpdateVersion.ReadyToUpdate.Default -> {
                val rpcUpdaterApi = withTimeoutOrNull(STOP_FEATURE_LOOKUP_TIMEOUT) {
                    fFeatureProvider.get<FRpcFeatureApi>()
                        .filterIsInstance<FFeatureStatus.Supported<*>>()
                        .filter { fFeatureStatus -> fFeatureStatus.featureApi is FRpcFeatureApi }
                        .filterIsInstance<FFeatureStatus.Supported<FRpcFeatureApi>>()
                        .first()
                        .featureApi
                        .fRpcUpdaterApi
                }
                if (rpcUpdaterApi == null) {
                    // Device vanished between the two lookups — release locally
                    installRequestedFlow.value = false
                    clearUpdatingDeviceId(updatingDeviceIdFlow.value, reason = "stopFirmwareUpdate (rpc unavailable)")
                    CResult.success(Unit)
                } else {
                    rpcUpdaterApi
                        .startUpdateAbortDownload()
                        .map { }
                        .also { installRequestedFlow.value = false }
                        .toCResult()
                        .onSuccess {
                            clearUpdatingDeviceId(updatingDeviceIdFlow.value, reason = "stopFirmwareUpdate (default)")
                        }
                }
            }

            is BsbUpdateVersion.ReadyToUpdate.Url -> {
                lanUpdaterScope.cancelPrevious().join()
                installRequestedFlow.value = false
                clearUpdatingDeviceId(updatingDeviceIdFlow.value, reason = "stopFirmwareUpdate (lan)")
                CResult.success(Unit)
            }

            else -> {
                installRequestedFlow.value = false
                clearUpdatingDeviceId(
                    updatingDeviceIdFlow.value,
                    reason = "stopFirmwareUpdate (no active version / device unreachable)"
                )
                CResult.success(Unit)
            }
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

    /**
     * Kicks off the install for the given version: Default — sends the install RPC to the
     * device, Url (LAN) — downloads the firmware and uploads it to the device. Passes the
     * version through so the caller can keep branching on it
     */
    private suspend fun dispatchInstall(
        bsbUpdateVersion: BsbUpdateVersion.ReadyToUpdate,
        onInstallRpcDispatch: () -> Unit = {}
    ): Result<BsbUpdateVersion.ReadyToUpdate> {
        return when (bsbUpdateVersion) {
            is BsbUpdateVersion.ReadyToUpdate.Default -> {
                val rpcUpdaterApi = fFeatureProvider.get<FRpcFeatureApi>()
                    .filterIsInstance<FFeatureStatus.Supported<*>>()
                    .filter { fFeatureStatus -> fFeatureStatus.featureApi is FRpcFeatureApi }
                    .filterIsInstance<FFeatureStatus.Supported<FRpcFeatureApi>>()
                    .first()
                    .featureApi
                    .fRpcUpdaterApi
                onInstallRpcDispatch()
                rpcUpdaterApi
                    .startUpdateInstall(bsbUpdateVersion.version)
                    .map { bsbUpdateVersion }
            }

            is BsbUpdateVersion.ReadyToUpdate.Url -> {
                firmwareDownloaderApi.download(bsbUpdateVersion)
                    .onSuccess { info { "#startUpdateInstall download finished" } }
                    .onFailure { t -> error(t) { "#startUpdateInstall could not download" } }
                    .mapSuspendCatching { path ->
                        info { "#startUpdateInstall start uploading" }
                        firmwareUploaderApi
                            .uploadAndInstall(path) { firmwareDownloaderApi.reset() }
                            .onSuccess { info { "#startUpdateInstall finish upload" } }
                            .onFailure { t -> error(t) { "#startUpdateInstall could not upload" } }
                            .getOrThrow()
                    }
                    .map { bsbUpdateVersion }
            }
        }
    }

    /**
     * Starts an update install depending on current [BsbUpdateVersion]
     * @see BsbUpdateVersion
     */
    override suspend fun startUpdateInstall(): CResult<Unit> {
        info { "#startUpdateInstall" }
        // deviceId and version come from a single emission, so the install below
        // is always attributed to the device that actually reported ReadyToUpdate
        val (updatingDeviceId, bsbUpdateVersion) = updateStatusProvider.getUpdateVersion()
            .mapNotNull { deviceUpdateVersion ->
                val version = deviceUpdateVersion?.version
                if (version is BsbUpdateVersion.ReadyToUpdate) {
                    deviceUpdateVersion.deviceId to version
                } else {
                    null
                }
            }
            .first()
        info { "#startUpdateInstall bsbUpdateVersion: $bsbUpdateVersion deviceId=$updatingDeviceId" }
        return when (bsbUpdateVersion) {
            is BsbUpdateVersion.ReadyToUpdate.Default -> {
                defaultUpdaterScope.async { runDefaultInstall(bsbUpdateVersion, updatingDeviceId) }.await()
            }

            is BsbUpdateVersion.ReadyToUpdate.Url -> {
                lanUpdaterScope.async { runLanInstall(bsbUpdateVersion, updatingDeviceId) }.await()
            }
        }
    }

    /**
     * Cloud/BLE install: the device downloads and installs the firmware itself, the app
     * only sends the install RPC. The job therefore ends right after the RPC round-trip;
     * [updatingDeviceIdFlow] stays set — the update is still running on the device
     */
    private suspend fun runDefaultInstall(
        bsbUpdateVersion: BsbUpdateVersion.ReadyToUpdate.Default,
        updatingDeviceId: String
    ): CResult<Unit> {
        beginInstall(updatingDeviceId)
        var rpcDispatched = false
        coroutineContext.job.invokeOnCompletion { cause ->
            // Cancelled (device switch) or died before the install RPC went out — no update
            // is running anywhere, release the gate and the id so Preparing can't leak.
            // After dispatch the outcome is unknown (the device may be installing), so the
            // id is conservatively kept; RPC failure below clears it explicitly
            if (cause != null && !rpcDispatched) {
                installRequestedFlow.value = false
                clearUpdatingDeviceId(updatingDeviceId, reason = "default install aborted before RPC (cause=$cause)")
            }
        }
        val updateResult = dispatchInstall(bsbUpdateVersion) { rpcDispatched = true }.toCResult()
        info { "#startUpdateInstall update result is $updateResult" }
        return updateResult
            .map { }
            .onFailure {
                // The install RPC never succeeded — no update is running, release the gate.
                installRequestedFlow.value = false
                clearUpdatingDeviceId(updatingDeviceId, reason = "default install RPC failed")
            }
    }

    /**
     * LAN install: the app itself downloads the firmware and feeds it to the device, so the
     * whole update lives inside this job — cancelling it (device switch, stop) aborts the
     * update
     */
    private suspend fun runLanInstall(
        bsbUpdateVersion: BsbUpdateVersion.ReadyToUpdate.Url,
        updatingDeviceId: String
    ): CResult<Unit> {
        beginInstall(updatingDeviceId)
        coroutineContext.job.invokeOnCompletion { cause ->
            installRequestedFlow.value = false
            firmwareDownloaderApi.reset()
            firmwareUploaderApi.reset()
            clearUpdatingDeviceId(updatingDeviceId, reason = "lan install job completed (cause=$cause)")
        }
        val bootTimeScope = coroutineContext.minusKey(Job)
            .plus(SupervisorJob(coroutineContext.job))
            .let(::CoroutineScope)
        try {
            val bootTimeFlow = getBootTimeFlow().shareIn(bootTimeScope, SharingStarted.Eagerly, 1)
            val updateResult = dispatchInstall(bsbUpdateVersion).toCResult()
            info { "#startUpdateInstall update result is $updateResult" }
            return updateResult.map {
                info { "#startUpdateInstall bootTimeFlow wait" }
                val bootTime = bootTimeFlow.first()
                info { "#startUpdateInstall bootTimeFlow cancel" }
                bootTimeScope.cancel()
                awaitDeviceReconnected(bootTime)
            }
        } finally {
            bootTimeScope.cancel()
        }
    }

    private fun beginInstall(updatingDeviceId: String) {
        installRequestedFlow.value = true
        updatingDeviceIdFlow.value = updatingDeviceId
        info { "#startUpdateInstall updatingDeviceId=$updatingDeviceId" }
        firmwareDownloaderApi.reset()
        firmwareUploaderApi.reset()
    }

    override suspend fun checkForUpdates(): CResult<Unit> {
        return fFeatureProvider.getSync<FRpcFeatureApi>()
            ?.fRpcUpdaterApi
            ?.startUpdateCheck()
            ?.map { }
            ?.toCResult()
            ?: CResult.failure(IllegalStateException("RPC feature is null"))
    }

    private fun clearUpdatingDeviceId(expectedDeviceId: String?, reason: String) {
        if (expectedDeviceId == null) return
        val cleared = updatingDeviceIdFlow.compareAndSet(expectedDeviceId, null)
        if (cleared) {
            info { "#clearUpdatingDeviceId $reason (was=$expectedDeviceId)" }
        }
    }

    // Reset updater when connected to another device
    init {
        fDevicePersistedStorage.getCurrentDeviceFlow()
            .map { bUSYBar -> bUSYBar?.uniqueId }
            .distinctUntilChanged()
            .onEach { deviceId ->
                info {
                    "#init current device changed to $deviceId, cancelling updater jobs " +
                        "(updatingDeviceId=${updatingDeviceIdFlow.value} kept)"
                }
                lanUpdaterScope.cancelPrevious().join()
                // A default install job is only alive while it waits for the RPC feature and
                // the round-trip; left running, a pre-RPC wait would resume against the NEXT
                // connected device and install A's firmware on B. A dispatched update runs
                // on the device itself and is not affected by this cancel
                defaultUpdaterScope.cancelPrevious().join()
                firmwareDownloaderApi.reset()
                firmwareUploaderApi.reset()
            }
            .launchIn(scope)

        // Release updatingDeviceId when the update actually ends. UpdateFinished/UpdateFailed
        // are detected via the version transition, which is only observable while connected
        // to the updating device — hence the connected == updating gate.
        eventsSharedFlow
            .onEach { event ->
                val updatingId = updatingDeviceIdFlow.value ?: return@onEach
                if (connectedDeviceIdFlow.value == updatingId) {
                    clearUpdatingDeviceId(updatingId, reason = "update event $event")
                }
            }
            .launchIn(scope)

        // Fallback for an update that finished while the user was on another device
        updateStatusProvider.getUpdateVersion()
            .onEach { value ->
                val updatingId = updatingDeviceIdFlow.value ?: return@onEach
                val updateDone = value != null &&
                    value.deviceId == updatingId &&
                    value.version is BsbUpdateVersion.NoUpdateAvailable
                if (updateDone) {
                    clearUpdatingDeviceId(updatingId, reason = "fresh NoUpdateAvailable on updating device")
                }
            }
            .launchIn(scope)
    }
}

/**
 * How long [FirmwareUpdaterApiImpl.stopFirmwareUpdate] waits for the connected device's
 * features before giving up and releasing the local tracking without a device round-trip
 */
private val STOP_FEATURE_LOOKUP_TIMEOUT = 5.seconds
