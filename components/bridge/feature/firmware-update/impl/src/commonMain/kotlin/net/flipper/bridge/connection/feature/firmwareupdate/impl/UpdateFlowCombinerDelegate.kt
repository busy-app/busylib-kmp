package net.flipper.bridge.connection.feature.firmwareupdate.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.get
import net.flipper.bridge.connection.feature.events.api.getMapped
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.firmwareupdate.model.AvailableVersion
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.util.toBsbUpdateStatus
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import net.flipper.busylib.core.wrapper.WrappedSharedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.merge
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info

class UpdateFlowCombinerDelegate(
    private val rpcFeatureApi: FRpcFeatureApi,
    fEventsFeatureApi: FEventsFeatureApi,
    private val scope: CoroutineScope
) {
    private val rpcUpdaterFlow = flow {
        val updateStatus = exponentialRetry {
            rpcFeatureApi.fRpcUpdaterApi
                .getUpdateStatus(ignoreCache = false)
                .onFailure { throwable -> error(throwable) { "Failed to get update status" } }
        }
        info { "Received update status $updateStatus" }
        emit(updateStatus)
    }.shareIn(scope = scope, SharingStarted.WhileSubscribed(), replay = 1)

    val availableVersionFlow = fEventsFeatureApi.get(scope, initial = {
        runSuspendCatching {
            rpcUpdaterFlow.first().let { response ->
                BusyLibUpdateEvent.Update.UpdateCheck(
                    availableVersion = when (response.check.status) {
                        UpdateStatus.Check.CheckResult.AVAILABLE -> response.check.availableVersion.ifBlank { null }
                        UpdateStatus.Check.CheckResult.NOT_AVAILABLE,
                        UpdateStatus.Check.CheckResult.FAILURE,
                        UpdateStatus.Check.CheckResult.NONE -> null
                    }
                )
            }
        }
    }, mapper = { flow ->
        flow.map { event ->
            val version = event.availableVersion
            if (version.isNullOrBlank()) {
                AvailableVersion.NotAvailable
            } else {
                AvailableVersion.Available(version)
            }
        }
    }).stateIn(scope, SharingStarted.WhileSubscribed(), AvailableVersion.Loading)

    private val updateDownloadEventsFlow = fEventsFeatureApi
        .get<BusyLibUpdateEvent.Update.UpdateDownload>()
        .map { it.busyLibUpdateEvent.toBsbUpdateStatus() }

    private val updateStateEventsFlow = fEventsFeatureApi
        .getMapped<BusyLibUpdateEvent.Update.UpdateState, BsbUpdateStatus>(scope, initial = {
            runSuspendCatching {
                rpcUpdaterFlow.first().toBsbUpdateStatus()
            }
        }, mapper = { it.toBsbUpdateStatus() })

    val updateStatusFlow: StateFlow<BsbUpdateStatus> = updateStateEventsFlow
        .merge(updateDownloadEventsFlow)
        .stateIn(scope, SharingStarted.WhileSubscribed(), BsbUpdateStatus.Loading)

}