package net.flipper.bridge.connection.transport.ble.api

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApi
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener

interface BleDeviceConnectionApi : DeviceConnectionApi<FBleApi, FBleDeviceConnectionConfig> {
    override suspend fun connect(
        scope: CoroutineScope,
        config: FBleDeviceConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): Result<FBleApi>
}
