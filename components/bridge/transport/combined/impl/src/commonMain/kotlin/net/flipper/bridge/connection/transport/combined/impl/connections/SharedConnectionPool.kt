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
import net.flipper.core.busylib.ktx.common.combine
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

data class ConnectionSnapshot(
    val capabilities: List<FHTTPTransportCapability>? = null,
    val status: FInternalTransportConnectionStatus,
)

class SharedConnectionPool(
    scope: CoroutineScope,
    connectionsFlow: Flow<List<AutoReconnectConnection>>,
) : LogTagProvider {
    override val TAG = "SharedConnectionPool"
    private fun getConnectionSnapshot(status: FInternalTransportConnectionStatus): Flow<ConnectionSnapshot> {
        info { "#getConnectionSnapshot status: $status" }
        if (status !is FInternalTransportConnectionStatus.Connected) {
            return flowOf(ConnectionSnapshot(status = status))
        }
        val deviceApi = status.deviceApi
        if (deviceApi !is FHTTPDeviceApi) {
            info { "#getConnectionSnapshot deviceApi is not FHTTPDeviceApi: $deviceApi" }
            return flowOf(ConnectionSnapshot(status = status))
        }
        return deviceApi.getCapabilities().map { capabilities ->
            ConnectionSnapshot(capabilities, status)
        }
    }

    private val sharedState = connectionsFlow.flatMapLatest { connections ->
        connections.map { connection ->
            connection
                .stateFlow
                .flatMapLatest(::getConnectionSnapshot)
        }.combine()
    }.shareIn(scope, SharingStarted.Eagerly, 1)

    fun get() = sharedState.asFlow()
}
