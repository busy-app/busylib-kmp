package net.flipper.transport.ble.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import net.flipper.bridge.connection.transport.ble.api.FSerialBleApi
import net.flipper.bridge.connection.transport.ble.common.ByteEndlessReadChannel
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.transport.ble.impl.cb.FPeripheralApi

class FSerialBleApiImpl(
    scope: CoroutineScope,
    val fPeripheralApi: FPeripheralApi
) : FSerialBleApi, LogTagProvider {
    override val TAG = "FSerialBleApi"
    private val channel = ByteEndlessReadChannel()

    init {
        fPeripheralApi
            .rxDataStream
            .onEach {
                channel.onByteReceive(it)
            }
            .launchIn(scope + FlipperDispatchers.default)
    }

    override fun getReceiveByteChannel() = channel

    override suspend fun send(data: ByteArray) {
        fPeripheralApi.writeValue(data)
    }
}
