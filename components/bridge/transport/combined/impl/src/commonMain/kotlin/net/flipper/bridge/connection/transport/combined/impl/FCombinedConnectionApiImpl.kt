package net.flipper.bridge.connection.transport.combined.impl

import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionApi
import net.flipper.bridge.connection.transport.combined.impl.connections.AutoReconnectConnection
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener

class FCombinedConnectionApiImpl(
    override val deviceName: String,
    private val connections: List<AutoReconnectConnection>,
    private val listener: FTransportConnectionStatusListener,
    scope: CoroutineScope
) : FCombinedConnectionApi {
    init {
        combine(connections.map { it.stateFlow }) { states ->
            states.maxBy { getPriority(it) }
        }.distinctUntilChanged()
            .onEach {
                if (it is FInternalTransportConnectionStatus.Connected) {
                    listener.onStatusUpdate(
                        FInternalTransportConnectionStatus.Connected(
                            scope = scope,
                            deviceApi = this
                        )
                    )
                } else {
                    listener.onStatusUpdate(it)
                }
            }.launchIn(scope)
    }

    override suspend fun tryUpdateConnectionConfig(config: FDeviceConnectionConfig<*>): Result<Unit> {
        return Result.failure(NotImplementedError())
    }

    private val httpEngine = FCombinedHttpEngine(scope, connections)

    override fun getDeviceHttpEngine(): HttpClientEngine {
        return httpEngine
    }

    override suspend fun disconnect() {
        connections.onEach {
            it.disconnect()
        }
    }
}

private fun getPriority(status: FInternalTransportConnectionStatus): Int {
    return when (status) {
        FInternalTransportConnectionStatus.Disconnected -> 0
        FInternalTransportConnectionStatus.Connecting -> 1
        FInternalTransportConnectionStatus.Disconnecting -> 2
        FInternalTransportConnectionStatus.Pairing -> 3
        is FInternalTransportConnectionStatus.Connected -> 4
    }
}
