package net.flipper.bridge.connection.transport.ble.impl.ios.central

import platform.Foundation.NSUUID

data class DiscoveredBluetoothDevice(
    val id: NSUUID,
    val name: String?
) {
    val address: String = id.UUIDString

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        if (other is DiscoveredBluetoothDevice) {
            return id == other.id
        }

        return false
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
