package net.flipper.bridge.connection.feature.firmwareupdate.updater.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.updater.model.FwUpdateState
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.mapCached
import net.flipper.core.busylib.ktx.common.orNullable
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, UpdaterApi::class)
class UpdaterApiImpl(
    private val fFeatureProvider: FFeatureProvider,
    private val scope: CoroutineScope
) : UpdaterApi, LogTagProvider by TaggedLogger("UpdaterApi") {

    private val changelogSharedFlow = fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
        .map { status ->
            status
                .tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>()
                ?.featureApi
        }
        .flatMapLatest { featureApi ->
            if (featureApi == null) return@flatMapLatest flowOf(null)
            featureApi.getUpdateStatusFlow()
                .filter { status ->
                    when (status.check.status) {
                        UpdateStatus.Check.CheckResult.NOT_AVAILABLE,
                        UpdateStatus.Check.CheckResult.AVAILABLE -> true

                        UpdateStatus.Check.CheckResult.NONE,
                        UpdateStatus.Check.CheckResult.FAILURE -> false
                    }
                }
                .map { status -> status.check.availableVersion }
                .distinctUntilChanged()
                .mapLatest { version ->
                    if (version.isEmpty()) {
                        null
                    } else {
                        exponentialRetry {
                            featureApi.getVersionChangelog(version)
                        }
                    }
                }
        }
        .shareIn(scope, SharingStarted.Eagerly, 1)

    override val state: StateFlow<FwUpdateState> = fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
        .map { status ->
            status
                .tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>()
                ?.featureApi
        }
        .flatMapLatest { feature -> feature?.getUpdateStatusFlow().orNullable() }
        .onEach { info { "#state status: $it" } }
        .flatMapLatest { updateStatus ->
            if (updateStatus == null) {
                return@flatMapLatest flowOf(FwUpdateState.Pending)
            }

            when (updateStatus.install.status) {
                UpdateStatus.Install.Status.BUSY,
                UpdateStatus.Install.Status.OK -> {
                    when (updateStatus.install.action) {
                        UpdateStatus.Install.Action.DOWNLOAD,
                        UpdateStatus.Install.Action.SHA_VERIFICATION,
                        UpdateStatus.Install.Action.UNPACK,
                        UpdateStatus.Install.Action.APPLY,
                        UpdateStatus.Install.Action.PREPARE -> {
                            changelogSharedFlow.map { changelogOrNull ->
                                FwUpdateState.Downloading(
                                    bsbVersionChangelog = changelogOrNull,
                                    progress = updateStatus.install.download.totalBytes
                                        .toFloat()
                                        .takeIf { total -> total > 0 }
                                        ?.let { total ->
                                            updateStatus.install
                                                .download
                                                .receivedBytes
                                                .div(total)
                                        }
                                        ?: 0f
                                )
                            }
                        }

                        UpdateStatus.Install.Action.NONE -> {
                            when (updateStatus.check.event) {
                                UpdateStatus.Check.CheckEvent.START,
                                UpdateStatus.Check.CheckEvent.NONE,
                                UpdateStatus.Check.CheckEvent.STOP -> {
                                    when (updateStatus.check.status) {
                                        UpdateStatus.Check.CheckResult.AVAILABLE -> {
                                            changelogSharedFlow.map { changelogOrNull ->
                                                FwUpdateState.UpdateAvailable(bsbVersionChangelog = changelogOrNull)
                                            }
                                        }

                                        UpdateStatus.Check.CheckResult.NOT_AVAILABLE -> {
                                            flowOf(FwUpdateState.NoUpdateAvailable)
                                        }

                                        UpdateStatus.Check.CheckResult.FAILURE -> {
                                            flowOf(FwUpdateState.CouldNotCheckUpdate)
                                        }

                                        UpdateStatus.Check.CheckResult.NONE -> {
                                            flowOf(FwUpdateState.CheckingVersion)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                UpdateStatus.Install.Status.BATTERY_LOW -> flowOf(FwUpdateState.LowBattery)
                UpdateStatus.Install.Status.DOWNLOAD_ABORT,
                UpdateStatus.Install.Status.SHA_MISMATCH,
                UpdateStatus.Install.Status.UNPACK_STAGING_DIR_FAILURE,
                UpdateStatus.Install.Status.UNPACK_ARCHIVE_OPEN_FAILURE,
                UpdateStatus.Install.Status.UNPACK_ARCHIVE_UNPACK_FAILURE,
                UpdateStatus.Install.Status.INSTALL_MANIFEST_NOT_FOUND,
                UpdateStatus.Install.Status.INSTALL_MANIFEST_INVALID,
                UpdateStatus.Install.Status.INSTALL_SESSION_CONFIG_FAILURE,
                UpdateStatus.Install.Status.INSTALL_POINTER_SETUP_FAILURE,
                UpdateStatus.Install.Status.UNKNOWN_FAILURE,
                UpdateStatus.Install.Status.DOWNLOAD_FAILURE -> flowOf(FwUpdateState.Failure)
            }
        }
        .onEach { info { "#state state: $it" } }
        .mapCached { currentFwUpdateState, previousFwUpdateState: FwUpdateState? ->
            when (previousFwUpdateState) {
                null -> currentFwUpdateState

                is FwUpdateState.Updating,
                is FwUpdateState.UpdateFinished,
                is FwUpdateState.UpdateAvailable,
                FwUpdateState.Pending,
                FwUpdateState.NoUpdateAvailable,
                FwUpdateState.LowBattery,
                FwUpdateState.Failure,
                FwUpdateState.CouldNotCheckUpdate,
                FwUpdateState.CheckingVersion,
                FwUpdateState.Busy -> currentFwUpdateState

                is FwUpdateState.Downloading -> {
                    when (currentFwUpdateState) {
                        is FwUpdateState.UpdateFinished,
                        FwUpdateState.Busy,
                        FwUpdateState.CheckingVersion,
                        FwUpdateState.CouldNotCheckUpdate,
                        is FwUpdateState.Downloading,
                        FwUpdateState.Failure,
                        FwUpdateState.LowBattery,
                        FwUpdateState.NoUpdateAvailable,
                        is FwUpdateState.Updating,
                        is FwUpdateState.UpdateAvailable -> currentFwUpdateState

                        FwUpdateState.Pending -> {
                            FwUpdateState.Updating(previousFwUpdateState.bsbVersionChangelog)
                        }
                    }
                }
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, FwUpdateState.Pending)

    private fun startUpdateCheckJob() {
        state
            .onEach { state ->
                when (state) {
                    is FwUpdateState.UpdateFinished,
                    is FwUpdateState.Updating,
                    is FwUpdateState.UpdateAvailable,
                    FwUpdateState.LowBattery,
                    FwUpdateState.NoUpdateAvailable,
                    is FwUpdateState.Downloading,
                    FwUpdateState.CheckingVersion,
                    FwUpdateState.Busy -> Unit

                    FwUpdateState.Pending,
                    FwUpdateState.Failure,
                    FwUpdateState.CouldNotCheckUpdate -> {
                        exponentialRetry {
                            fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
                                .filterIsInstance<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>()
                                .first()
                                .featureApi
                                .startUpdateCheck()
                        }
                    }
                }
            }
            .launchIn(scope)
    }

    init {
        startUpdateCheckJob()
    }
}
