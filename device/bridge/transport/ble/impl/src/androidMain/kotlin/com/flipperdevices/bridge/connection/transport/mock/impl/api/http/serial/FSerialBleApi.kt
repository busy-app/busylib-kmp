package com.flipperdevices.bridge.connection.transport.mock.impl.api.http.serial

import com.flipperdevices.core.ktx.common.FlipperDispatchers
import com.flipperdevices.core.log.LogTagProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus

class FSerialBleApi(
    scope: CoroutineScope,
    private val unsafeSerialApi: FSerialUnsafeApiImpl,
) : LogTagProvider {
    override val TAG = "FSerialBleApi"
    private val channel = ByteEndlessReadChannel()

    init {
        unsafeSerialApi
            .getReceiveBytesFlow()
            .onEach {
                channel.onByteReceive(it)
            }
            .launchIn(scope + FlipperDispatchers.default)
    }

    fun getReceiveByteChannel() = channel

    /**
     * @return the first ble response after send request
     */
    suspend fun send(data: ByteArray) {
        unsafeSerialApi.sendBytes(data)
    }
}
