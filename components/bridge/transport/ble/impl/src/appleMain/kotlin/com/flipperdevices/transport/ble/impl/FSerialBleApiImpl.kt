package com.flipperdevices.transport.ble.impl

import com.flipperdevices.bridge.connection.transport.ble.api.FSerialBleApi
import com.flipperdevices.bridge.connection.transport.common.utils.ByteEndlessReadChannel
import com.flipperdevices.core.ktx.common.FlipperDispatchers
import com.flipperdevices.core.log.LogTagProvider
import com.flipperdevices.transport.ble.impl.cb.FPeripheralApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus

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
                channel.onByteReceive(it.toByteArray())
            }
            .launchIn(scope + FlipperDispatchers.default)
    }

    override fun getReceiveByteChannel() = channel

    override suspend fun send(data: ByteArray) {
        fPeripheralApi.writeValue(data.toNSData())
    }
}