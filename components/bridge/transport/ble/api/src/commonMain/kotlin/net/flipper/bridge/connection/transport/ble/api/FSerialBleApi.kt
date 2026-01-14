package net.flipper.bridge.connection.transport.ble.api

import net.flipper.bridge.connection.transport.ble.common.ByteEndlessReadChannel

interface FSerialBleApi {
    fun getReceiveByteChannel(): ByteEndlessReadChannel
    suspend fun send(data: ByteArray)
}
