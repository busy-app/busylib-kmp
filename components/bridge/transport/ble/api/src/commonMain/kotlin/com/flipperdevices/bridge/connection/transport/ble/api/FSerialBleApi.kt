package com.flipperdevices.bridge.connection.transport.ble.api

import com.flipperdevices.bridge.connection.transport.ble.common.ByteEndlessReadChannel

interface FSerialBleApi {
    fun getReceiveByteChannel(): ByteEndlessReadChannel
    suspend fun send(data: ByteArray)
}
