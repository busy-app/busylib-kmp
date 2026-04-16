package net.flipper.bridge.connection.transport.combined.impl

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.combined.CombinedConnectionApi
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionApi
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionConfig
import net.flipper.bridge.connection.transport.combined.impl.connections.AutoReconnectConnection
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(BusyLibGraph::class, CombinedConnectionApi::class)
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
