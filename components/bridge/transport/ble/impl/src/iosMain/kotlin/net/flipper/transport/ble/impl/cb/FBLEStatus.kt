package net.flipper.transport.ble.impl.cb

import platform.CoreBluetooth.CBManagerState

enum class FBLEStatus(val rawValue: Long) {
    UNKNOWN(rawValue = 0),
    RESETTING(rawValue = 1),
    UNSUPPORTED(rawValue = 2),
    UNAUTHORIZED(rawValue = 3),
    POWERED_OFF(rawValue = 4),
    POWERED_ON(rawValue = 5);

    companion object {
        fun from(state: CBManagerState): FBLEStatus {
            return entries.find { it.rawValue == state } ?: UNKNOWN
        }
    }
}
