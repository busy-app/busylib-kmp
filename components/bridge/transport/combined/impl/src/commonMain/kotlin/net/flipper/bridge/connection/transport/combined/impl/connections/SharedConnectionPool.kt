package net.flipper.bridge.connection.transport.combined.impl.connections

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.core.busylib.ktx.common.asFlow
import net.flipper.core.busylib.log.LogTagProvider

data class ConnectionSnapshot(
    val capabilities: List<FHTTPTransportCapability>? = null,
    val status: FInternalTransportConnectionStatus,
)

class SharedConnectionPool(
    scope: CoroutineScope,
    connectionsFlow: Flow<List<AutoReconnectConnection>>,
) : LogTagProvider {
    override val TAG = "SharedConnectionPool"
    private val sharedState = connectionsFlow.flatMapLatest { connections ->
        if (connections.isEmpty()) {
            flowOf(emptyArray())
        } else {
            combine(
                connections.map { connection ->
                    connection.stateFlow.flatMapLatest { status ->
                        getConnectionSnapshot(status)
                    }
                }
            ) { it }
        }
    }.shareIn(scope, SharingStarted.Eagerly, 1)

    fun get() = sharedState.asFlow()
}

private fun getConnectionSnapshot(status: FInternalTransportConnectionStatus): Flow<ConnectionSnapshot> {
    if (status !is FInternalTransportConnectionStatus.Connected) {
        return flowOf(ConnectionSnapshot(status = status))
    }
    val deviceApi = status.deviceApi
    if (deviceApi !is FHTTPDeviceApi) {
        return flowOf(ConnectionSnapshot(status = status))
    }
    return deviceApi.getCapabilities().map { capabilities ->
        ConnectionSnapshot(capabilities, status)
    }
}
