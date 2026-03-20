package net.flipper.bridge.connection.transport.ble.impl.ios.peripheral

import platform.CoreBluetooth.CBUUID
import kotlin.uuid.Uuid

internal fun CBUUID.toKotlinUUID(): Uuid {
    return Uuid.parse(normalizeBluetoothUuid(UUIDString))
}
