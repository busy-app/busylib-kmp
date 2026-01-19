package net.flipper.bridge.connection.transport.combined

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApi
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener

interface CombinedConnectionApi : DeviceConnectionApi<FCombinedConnectionApi, FCombinedConnectionConfig> {
    override suspend fun connect(
        scope: CoroutineScope,
        config: FCombinedConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): Result<FCombinedConnectionApi>
}