package net.flipper.bridge.connection.transport.ble.impl.serial

import kotlinx.coroutines.flow.Flow

interface FResetSerialBleApi {
    fun getRequestCounterStateFlow(): Flow<Int>
    suspend fun reset()
}
