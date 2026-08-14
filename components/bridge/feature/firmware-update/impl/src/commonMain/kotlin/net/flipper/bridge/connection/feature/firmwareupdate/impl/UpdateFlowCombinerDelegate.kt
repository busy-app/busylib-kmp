package net.flipper.bridge.connection.feature.firmwareupdate.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.getMapped
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.firmwareupdate.model.AvailableVersion
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.util.toAvailableVersion
import net.flipper.bridge.connection.feature.firmwareupdate.util.toBsbUpdateStatus
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info

class UpdateFlowCombinerDelegate(
    private val rpcFeatureApi: FRpcFeatureApi,
    fEventsFeatureApi: FEventsFeatureApi,
    private val scope: CoroutineScope
) : LogTagProvider {
    override val TAG = "UpdateFlowCombinerDelegate"

    private suspend fun getUpdateStatus(): UpdateStatus {
        val updateStatus = exponentialRetry {
            rpcFeatureApi.fRpcUpdaterApi
                .getUpdateStatus(ignoreCache = false)
                .onFailure { throwable -> error(throwable) { "Failed to get update status" } }
        }
        info { "Received update status $updateStatus" }
        return updateStatus
    }

    val availableVersionFlow = fEventsFeatureApi
        .getMapped<BusyLibUpdateEvent.Update.UpdateCheck, AvailableVersion>(
            scope = scope,
            initial = {
                runSuspendCatching { getUpdateStatus().toAvailableVersion() }
            },
            mapper = { updateCheck ->
                val result = updateCheck.toAvailableVersion()
                debug { "From version $updateCheck map to $result" }
                return@getMapped result
            }
        ).stateIn(scope, SharingStarted.WhileSubscribed(), AvailableVersion.Loading)

    private val updateStateEventsFlow = fEventsFeatureApi
        .getMapped<BusyLibUpdateEvent.Update.UpdateState, BsbUpdateStatus>(
            scope = scope,
            initial = {
                runSuspendCatching { getUpdateStatus().toBsbUpdateStatus() }
            },
            mapper = { updateState ->
                val result = updateState.toBsbUpdateStatus()
                debug { "From update $updateState map to $result" }
                return@getMapped result
            }
        )

    val updateStatusFlow: StateFlow<BsbUpdateStatus> = updateStateEventsFlow
        .onEach { bsbUpdateStatus -> debug { "Update status publish: $bsbUpdateStatus" } }
        .stateIn(scope, SharingStarted.WhileSubscribed(), BsbUpdateStatus.Loading)
}
