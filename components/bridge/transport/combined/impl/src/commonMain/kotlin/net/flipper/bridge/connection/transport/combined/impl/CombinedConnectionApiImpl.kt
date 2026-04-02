package net.flipper.bridge.connection.transport.combined.impl

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.combined.CombinedConnectionApi
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionApi
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionConfig
import net.flipper.bridge.connection.transport.combined.impl.connections.AutoReconnectConnection
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding

@ContributesBinding(BusyLibGraph::class, binding = binding<CombinedConnectionApi>())
class CombinedConnectionApiImpl : CombinedConnectionApi {
    override suspend fun connect(
        scope: CoroutineScope,
        config: FCombinedConnectionConfig,
        listener: FTransportConnectionStatusListener,
        connectionBuilder: FDeviceConfigToConnection
    ): Result<FCombinedConnectionApi> = runCatching {
        listener.onStatusUpdate(FInternalTransportConnectionStatus.Connecting)

        val connections = config.connectionConfigs.map { connectionConfig ->
            AutoReconnectConnection(
                scope = scope,
                initialConfig = connectionConfig,
                connectionBuilder = connectionBuilder
            )
        }
        return@runCatching FCombinedConnectionApiImpl(
            scope = scope,
            initialConnections = connections,
            listener = listener,
            currentConfig = config,
            connectionBuilder = connectionBuilder
        )
    }
}
