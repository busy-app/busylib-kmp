package net.flipper.transport.ble.impl.manager

import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.transport.ble.impl.cb.FBLEStatus
import net.flipper.transport.ble.impl.cb.FPeripheralApi
import platform.Foundation.NSUUID

interface FCentralManagerApi {
    val connectedStream: WrappedStateFlow<Map<NSUUID, FPeripheralApi>>
    val bleStatusStream: WrappedStateFlow<FBLEStatus>
    val discoveredStream: WrappedStateFlow<Set<NSUUID>>

    suspend fun connect(config: FBleDeviceConnectionConfig)
    suspend fun disconnect(id: NSUUID)
    suspend fun startScan()
    suspend fun stopScan()
}