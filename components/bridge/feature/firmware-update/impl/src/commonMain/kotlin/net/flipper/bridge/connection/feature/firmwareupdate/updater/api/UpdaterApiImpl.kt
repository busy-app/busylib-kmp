package net.flipper.bridge.connection.feature.firmwareupdate.updater.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
        .combine(changelogSharedFlow) { updateStatus, changelogOrNull ->
            FwUpdateStatusMapper.toFwUpdateState(
                updateStatus = updateStatus,
                changelogOrNull = changelogOrNull
            )
        }
        .onEach { info { "#state state: $it" } }
        .mapCached { currentFwUpdateState, previousFwUpdateState: FwUpdateState? ->
            FwUpdateStateDiff.combineDiff(
                previous = previousFwUpdateState,
                latest = currentFwUpdateState
            )
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
