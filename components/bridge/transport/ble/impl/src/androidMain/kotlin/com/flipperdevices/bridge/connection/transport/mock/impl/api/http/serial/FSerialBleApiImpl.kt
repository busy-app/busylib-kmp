package com.flipperdevices.bridge.connection.transport.mock.impl.api.http.serial

import com.flipperdevices.core.busylib.ktx.common.FlipperDispatchers
import com.flipperdevices.core.busylib.log.LogTagProvider
import com.flipperdevices.bridge.connection.transport.ble.api.FSerialBleApi
import com.flipperdevices.bridge.connection.transport.ble.common.ByteEndlessReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus

class FSerialBleApiImpl(
    scope: CoroutineScope,
    private val unsafeSerialApi: FSerialUnsafeApiImpl,
) : FSerialBleApi, LogTagProvider {
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

    override fun getReceiveByteChannel() = channel

    /**
     * @return the first ble response after send request
     */
    override suspend fun send(data: ByteArray) {
        unsafeSerialApi.sendBytes(data)
    }
}
