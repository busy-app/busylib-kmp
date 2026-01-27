package net.flipper.bridge.connection.transport.combined

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener

interface CombinedConnectionApi {
    suspend fun connect(
        scope: CoroutineScope,
        config: FCombinedConnectionConfig,
        listener: FTransportConnectionStatusListener,
        connectionBuilder: FDeviceConfigToConnection
    ): Result<FCombinedConnectionApi>
}
