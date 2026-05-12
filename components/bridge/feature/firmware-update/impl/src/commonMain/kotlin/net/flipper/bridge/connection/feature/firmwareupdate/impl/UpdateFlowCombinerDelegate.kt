package net.flipper.bridge.connection.feature.firmwareupdate.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
import net.flipper.core.busylib.log.debug
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

    val availableVersionFlow = fEventsFeatureApi
        .getMapped<BusyLibUpdateEvent.Update.UpdateCheck, AvailableVersion>(
            scope = scope,
            initial = {
                runSuspendCatching {
                    rpcUpdaterFlow.first().toAvailableVersion()
                }
            },
            mapper = {
                it.toAvailableVersion()
            }
        ).onEach {
            debug { "Available version: $it" }
        }.stateIn(scope, SharingStarted.WhileSubscribed(), AvailableVersion.Loading)

    private val updateDownloadEventsFlow = fEventsFeatureApi
        .get<BusyLibUpdateEvent.Update.UpdateDownload>()
        .map { it.busyLibUpdateEvent.toBsbUpdateStatus() }
        .onEach {
            debug { "Download flow: $it" }
        }

    private val updateStateEventsFlow = fEventsFeatureApi
        .getMapped<BusyLibUpdateEvent.Update.UpdateState, BsbUpdateStatus>(scope, initial = {
            runSuspendCatching {
                rpcUpdaterFlow.first().toBsbUpdateStatus()
            }
        }, mapper = { it.toBsbUpdateStatus() })
        .onEach {
            debug { "Update status: $it" }
        }

    val updateStatusFlow: StateFlow<BsbUpdateStatus> = updateStateEventsFlow
        .merge(updateDownloadEventsFlow)
        .onEach {
            debug { "Update status publish: $it" }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(), BsbUpdateStatus.Loading)
}
