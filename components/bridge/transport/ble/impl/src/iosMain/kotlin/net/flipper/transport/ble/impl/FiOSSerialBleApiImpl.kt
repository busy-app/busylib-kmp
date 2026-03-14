package net.flipper.transport.ble.impl

import io.ktor.utils.io.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import net.flipper.bridge.connection.transport.ble.impl.serial.ByteEndlessReadChannel
import net.flipper.bridge.connection.transport.ble.impl.serial.FSerialBleApi
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.ktx.common.launchOnCompletion
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.transport.ble.impl.cb.FPeripheralApi

class FiOSSerialBleApiImpl(
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

        scope.launchOnCompletion { channel.cancel() }
    }

    override fun getReceiveByteChannel() = channel

    override suspend fun send(data: ByteArray) {
        fPeripheralApi.writeValue(data)
    }

    // Noop for iOS
    override fun getRequestCounterStateFlow(): StateFlow<Int> {
        return MutableStateFlow(0)
    }

    override suspend fun reset() {
        error { "Tried to reset, but this is noop implementation!" }
    }
}
