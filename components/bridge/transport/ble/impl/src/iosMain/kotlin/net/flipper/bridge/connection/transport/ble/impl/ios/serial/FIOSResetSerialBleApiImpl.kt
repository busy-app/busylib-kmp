package net.flipper.bridge.connection.transport.ble.impl.ios.serial

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.ble.api.RESET_CHARACTERISTICS_REPLAY_VALUE
import net.flipper.bridge.connection.transport.ble.impl.BleConstants.POLLING_RESET_INTERVAL
import net.flipper.bridge.connection.transport.ble.impl.ios.peripheral.FPeripheralApi
import net.flipper.bridge.connection.transport.ble.impl.serial.FResetSerialBleApi
import net.flipper.bridge.connection.transport.ble.impl.toRequestCounter
import net.flipper.bridge.connection.transport.ble.impl.toUInt32ByteArray
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.info

class FIOSResetSerialBleApiImpl(
    scope: CoroutineScope,
    private val fPeripheralApi: FPeripheralApi,
    private val config: FBleDeviceConnectionConfig,
) : FResetSerialBleApi, LogTagProvider {
    override val TAG = "FResetSerialBleApi"

    private val requestCounterFlow = flow {
        while (currentCoroutineContext().isActive) {
            val counter = fPeripheralApi
                .readValue(config.serialConfig.resetCharUuid)
                .toRequestCounter()
            debug { "Receive request counter $counter" }
            emit(counter)
            delay(POLLING_RESET_INTERVAL)
        }
    }.shareIn(scope, SharingStarted.Eagerly, RESET_CHARACTERISTICS_REPLAY_VALUE)

    override fun getRequestCounterFlow(): Flow<Int> {
        return requestCounterFlow
    }

    override suspend fun reset() {
        fPeripheralApi.writeValue(
            characteristicUuid = config.serialConfig.resetCharUuid,
            data = 0.toUInt32ByteArray(),
        )
        info { "Reset command written, waiting for request counter to be zero" }
        requestCounterFlow.filter { it == 0 }.first()
        info { "Reset success" }
    }
}
