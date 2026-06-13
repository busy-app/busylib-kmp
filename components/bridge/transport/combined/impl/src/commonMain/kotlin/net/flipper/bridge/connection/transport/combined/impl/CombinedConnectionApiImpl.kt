package net.flipper.bridge.connection.transport.combined.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.combined.CombinedConnectionApi
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionApi
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionConfig
import net.flipper.bridge.connection.transport.combined.impl.connections.AutoReconnectConnection
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.runSuspendCatching

@Inject
@ContributesBinding(BusyLibGraph::class, binding = binding<CombinedConnectionApi>())
class CombinedConnectionApiImpl : CombinedConnectionApi {
    override suspend fun connect(
        scope: CoroutineScope,
        config: FCombinedConnectionConfig,
        listener: FTransportConnectionStatusListener,
        connectionBuilder: FDeviceConfigToConnection
    ): Result<FCombinedConnectionApi> = runSuspendCatching {
        listener.onStatusUpdate(FInternalTransportConnectionStatus.Connecting(config.getTransportTypes()))

        val connections = config.connectionConfigs.map { connectionConfig ->
            AutoReconnectConnection(
                scope = scope,
                initialConfig = connectionConfig,
                connectionBuilder = connectionBuilder
            )
        }
        return@runSuspendCatching FCombinedConnectionApiImpl(
            scope = scope,
            initialConnections = connections,
            listener = listener,
            currentConfig = config,
            connectionBuilder = connectionBuilder
        )
    }
}
