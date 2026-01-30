package net.flipper.bridge.connection.feature.firmwareupdate.updater.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.updater.model.FwUpdateState
import net.flipper.bridge.connection.feature.firmwareupdate.updater.service.CheckUpdateService
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.feature.provider.api.getSync
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarVersion
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap
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
    private val scope: CoroutineScope,
    private val checkUpdateService: CheckUpdateService,
    private val availableVersionChangelogProvider: AvailableVersionChangelogProvider
) : UpdaterApi, LogTagProvider by TaggedLogger("UpdaterApi") {

    private val changelogSharedFlow = availableVersionChangelogProvider.getLatestAvailableChangelogFlow()
        .shareIn(scope, SharingStarted.Eagerly, 1)

    override val state: WrappedStateFlow<FwUpdateState> = fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
        .map { status ->
            status
                .tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>()
                ?.featureApi
        }
        .flatMapLatest { feature -> feature?.getUpdateStatusFlow().orNullable() }
        .combine(changelogSharedFlow) { updateStatus, changelogOrNull ->
            FwUpdateStatusMapper.toFwUpdateState(
                updateStatus = updateStatus,
                changelogOrNull = changelogOrNull
            )
        }
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

    init {
        checkUpdateService.onEnable()
    }
}
