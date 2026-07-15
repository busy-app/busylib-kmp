package net.flipper.bridge.connection.feature.firmwareupdate.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.get
import net.flipper.bridge.connection.feature.events.api.getMapped
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.firmwareupdate.model.AvailableVersion
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.util.toAvailableVersion
import net.flipper.bridge.connection.feature.firmwareupdate.util.toBsbUpdateStatus
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.merge
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info

class UpdateFlowCombinerDelegate(
    private val rpcFeatureApi: FRpcFeatureApi,
    private val fEventsFeatureApi: FEventsFeatureApi,
    private val scope: CoroutineScope
) : LogTagProvider {
    override val TAG = "UpdateFlowCombinerDelegate"
    private val rpcUpdaterFlow = flow {
        val updateStatus = exponentialRetry {
            rpcFeatureApi.fRpcUpdaterApi
                .getUpdateStatus(ignoreCache = false)
                .onFailure { throwable -> error(throwable) { "Failed to get update status" } }
        }
        info { "Received update status $updateStatus" }
        emit(updateStatus)
    }.shareIn(scope = scope, SharingStarted.WhileSubscribed(), replay = 1)

    val availableVersionFlow = fEventsFeatureApi
        .getMapped<BusyLibUpdateEvent.Update.UpdateCheck, AvailableVersion>(
            scope = scope,
            initial = {
                runSuspendCatching {
                    rpcUpdaterFlow.first().toAvailableVersion()
                }
            },
            mapper = {
                val result = it.toAvailableVersion()
                debug { "From version $it map to $result" }
                return@getMapped result
            }
        ).stateIn(scope, SharingStarted.WhileSubscribed(), AvailableVersion.Loading)

    private fun asd() {


    }

    private val updateStateEventsFlow = fEventsFeatureApi
        .getMapped<BusyLibUpdateEvent.Update.UpdateState, BsbUpdateStatus>(
            scope = scope,
            initial = {
                runSuspendCatching {
                    rpcUpdaterFlow.first().toBsbUpdateStatus()
                }
            }, mapper = { updateState ->
                val result = updateState.toBsbUpdateStatus()
                debug { "From update $updateState map to $result" }
                return@getMapped result
            }
        )
        .flatMapLatest { bsbUpdateStatus ->
            fEventsFeatureApi.get<BusyLibUpdateEvent.Update.BatteryStateChanged>()
                .map { event ->
                    when (bsbUpdateStatus) {
                        is BsbUpdateStatus.InProgress.Downloading.Specified,
                        is BsbUpdateStatus.InProgress.Other,
                        BsbUpdateStatus.Loading,
                        BsbUpdateStatus.ReadyToInstall.BatteryLow,
                        BsbUpdateStatus.InProgress.Downloading.NotSpecified -> bsbUpdateStatus

                        BsbUpdateStatus.ReadyToInstall.Ready -> {
                            if (event.busyLibUpdateEvent.isLowBattery) BsbUpdateStatus.ReadyToInstall.BatteryLow
                            else bsbUpdateStatus
                        }
                    }
                }
                .merge(flowOf(bsbUpdateStatus))
        }
    val updateStatusFlow: StateFlow<BsbUpdateStatus> = updateStateEventsFlow
        .onEach { bsbUpdateStatus -> debug { "Update status publish: $bsbUpdateStatus" } }
        .stateIn(scope, SharingStarted.WhileSubscribed(), BsbUpdateStatus.Loading)
}
