package net.flipper.bridge.connection.transport.ble.impl.serial

interface FSerialBleApi {
    fun getReceiveByteChannel(): ByteEndlessReadChannel
    suspend fun send(data: ByteArray)
}