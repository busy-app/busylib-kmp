package net.flipper.bridge.connection.transport.ble.impl.ios.serial

import io.ktor.utils.io.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import net.flipper.bridge.connection.transport.ble.impl.ios.peripheral.FPeripheralApi
import net.flipper.bridge.connection.transport.ble.impl.serial.ByteEndlessReadChannel
import net.flipper.bridge.connection.transport.ble.impl.serial.FResetSerialBleApi
import net.flipper.bridge.connection.transport.ble.impl.serial.FSerialBleApi
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.ktx.common.launchOnCompletion
import net.flipper.core.busylib.log.LogTagProvider

class FIOSSerialBleApiImpl(
    scope: CoroutineScope,
    val fPeripheralApi: FPeripheralApi,
    private val resetApi: FResetSerialBleApi
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

    override fun getRequestCounterFlow() = resetApi.getRequestCounterFlow()

    override suspend fun reset() {
        resetApi.reset()
        channel.clear()
    }
}
