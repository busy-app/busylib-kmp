package net.flipper.bridge.connection.transport.mock.impl.api.http.serial

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import net.flipper.bridge.connection.transport.ble.api.FSerialBleApi
import net.flipper.bridge.connection.transport.ble.common.ByteEndlessReadChannel
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.log.LogTagProvider

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
