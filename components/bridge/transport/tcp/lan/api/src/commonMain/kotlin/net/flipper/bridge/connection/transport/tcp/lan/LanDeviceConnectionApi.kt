package net.flipper.bridge.connection.transport.tcp.lan

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApi
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener

interface LanDeviceConnectionApi : DeviceConnectionApi<FLanApi, FLanDeviceConnectionConfig> {
    override suspend fun connect(
        scope: CoroutineScope,
        config: FLanDeviceConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): Result<FLanApi>
}
