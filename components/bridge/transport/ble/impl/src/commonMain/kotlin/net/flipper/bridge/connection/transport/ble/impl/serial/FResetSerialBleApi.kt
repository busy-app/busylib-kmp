package net.flipper.bridge.connection.transport.ble.impl.serial

import kotlinx.coroutines.flow.StateFlow

interface FResetSerialBleApi {
    fun getRequestCounterStateFlow(): StateFlow<Int>
    suspend fun reset()
}
