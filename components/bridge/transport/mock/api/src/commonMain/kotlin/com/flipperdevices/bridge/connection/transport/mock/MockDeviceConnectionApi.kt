package com.flipperdevices.bridge.connection.transport.mock

import com.flipperdevices.bridge.connection.transport.common.api.DeviceConnectionApi
import com.flipperdevices.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import kotlinx.coroutines.CoroutineScope

interface MockDeviceConnectionApi : DeviceConnectionApi<FMockApi, FMockDeviceConnectionConfig> {
    override suspend fun connect(
        scope: CoroutineScope,
        config: FMockDeviceConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): Result<FMockApi>
}
