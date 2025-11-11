package net.flipper.bridge.connection.transport.mock

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApi
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener

interface MockDeviceConnectionApi : DeviceConnectionApi<FMockApi, FMockDeviceConnectionConfig> {
    override suspend fun connect(
        scope: CoroutineScope,
        config: FMockDeviceConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): Result<FMockApi>
}
