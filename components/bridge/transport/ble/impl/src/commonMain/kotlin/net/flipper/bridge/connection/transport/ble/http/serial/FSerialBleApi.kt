package net.flipper.bridge.connection.transport.ble.http.serial

interface FSerialBleApi {
    fun getReceiveByteChannel(): ByteEndlessReadChannel
    suspend fun send(data: ByteArray)
}