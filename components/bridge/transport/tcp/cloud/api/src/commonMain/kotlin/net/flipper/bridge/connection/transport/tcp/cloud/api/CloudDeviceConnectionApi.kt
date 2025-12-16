package net.flipper.bridge.connection.transport.tcp.cloud.api

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApi
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener

interface CloudDeviceConnectionApi : DeviceConnectionApi<FCloudApi, FCloudDeviceConnectionConfig> {
    override suspend fun connect(
        scope: CoroutineScope,
        config: FCloudDeviceConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): Result<FCloudApi>
}
