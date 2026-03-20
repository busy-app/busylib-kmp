package net.flipper.bridge.connection.transport.ble.impl.ios.serial

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
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

    private val requestCounterStateFlow = flow {
        while (currentCoroutineContext().isActive) {
            val counter = (
                fPeripheralApi
                    .readValue(config.serialConfig.resetCharUuid)
                )
                .toRequestCounter()
            debug { "Receive request counter $counter" }
            emit(counter)
            delay(POLLING_RESET_INTERVAL)
        }
    }.stateIn(scope, SharingStarted.Eagerly, 0)

    override fun getRequestCounterStateFlow(): StateFlow<Int> {
        return requestCounterStateFlow
    }

    override suspend fun reset() {
        fPeripheralApi.writeValue(
            characteristicUuid = config.serialConfig.resetCharUuid,
            data = 0.toUInt32ByteArray(),
        )
        info { "Reset command written, waiting for request counter to be zero" }
        requestCounterStateFlow.filter { it == 0 }.first()
        info { "Reset success" }
    }
}
