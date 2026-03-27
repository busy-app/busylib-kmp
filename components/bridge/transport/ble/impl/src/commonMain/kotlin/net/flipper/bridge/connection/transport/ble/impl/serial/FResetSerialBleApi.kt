package net.flipper.bridge.connection.transport.ble.impl.serial

import kotlinx.coroutines.flow.Flow

interface FResetSerialBleApi {
    fun getRequestCounterFlow(): Flow<Int>
    suspend fun reset()
}
