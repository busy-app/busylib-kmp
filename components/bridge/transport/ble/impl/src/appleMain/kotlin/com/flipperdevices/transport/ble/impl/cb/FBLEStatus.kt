package com.flipperdevices.transport.ble.impl.cb

import platform.CoreBluetooth.CBManagerState

enum class FBLEStatus(val rawValue: Long) {
    UNKNOWN(0),
    RESETTING(1),
    UNSUPPORTED(2),
    UNAUTHORIZED(3),
    POWERED_OFF(4),
    POWERED_ON(5);

    companion object {
        fun from(state: CBManagerState): FBLEStatus {
            return entries.find { it.rawValue == state } ?: UNKNOWN
        }
    }
}