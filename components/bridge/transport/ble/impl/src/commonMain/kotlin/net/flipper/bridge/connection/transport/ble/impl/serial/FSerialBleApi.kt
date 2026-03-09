package net.flipper.bridge.connection.transport.ble.impl.serial

import io.ktor.utils.io.ByteReadChannel

interface FSerialBleApi : FResetSerialBleApi {
    fun getReceiveByteChannel(): ByteReadChannel
    suspend fun send(data: ByteArray)
}
