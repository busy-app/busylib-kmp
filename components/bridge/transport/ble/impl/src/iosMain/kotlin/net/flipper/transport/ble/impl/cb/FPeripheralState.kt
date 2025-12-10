package net.flipper.transport.ble.impl.cb

import platform.CoreBluetooth.CBPeripheralState

enum class FPeripheralState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    PAIRING_FAILED,
    INVALID_PAIRING;

    companion object {
        fun from(state: CBPeripheralState): FPeripheralState {
            return when (state) {
                0L -> DISCONNECTED
                1L -> CONNECTING
                2L -> CONNECTED
                3L -> DISCONNECTING
                else -> DISCONNECTED
            }
        }
    }
}
