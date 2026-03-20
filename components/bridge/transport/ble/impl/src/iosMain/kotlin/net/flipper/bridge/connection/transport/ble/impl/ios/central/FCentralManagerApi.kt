package net.flipper.bridge.connection.transport.ble.impl.ios.central

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.ble.impl.ios.peripheral.FPeripheralApi
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import platform.Foundation.NSUUID

interface FCentralManagerApi {
    val connectedStream: WrappedStateFlow<Map<NSUUID, FPeripheralApi>>
    val bleStatusStream: WrappedStateFlow<FBLEStatus>
    val discoveredStream: WrappedStateFlow<Set<DiscoveredBluetoothDevice>>

    suspend fun connect(scope: CoroutineScope, config: FBleDeviceConnectionConfig)
    suspend fun disconnect(id: NSUUID)
    suspend fun startScan()
    suspend fun stopScan()
}
