package net.flipper.bridge.connection.ble.impl

import net.flipper.bridge.connection.orchestrator.api.model.FDeviceTransportType
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType

fun FInternalTransportConnectionType.toPublic(): FDeviceTransportType {
    return when (this) {
        FInternalTransportConnectionType.BLE -> FDeviceTransportType.BLE
        FInternalTransportConnectionType.LAN -> FDeviceTransportType.LAN
        FInternalTransportConnectionType.MOCK,
        FInternalTransportConnectionType.CLOUD -> FDeviceTransportType.CLOUD
    }
}
