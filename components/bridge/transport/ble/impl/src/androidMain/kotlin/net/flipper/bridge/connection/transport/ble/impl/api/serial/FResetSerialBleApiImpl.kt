package net.flipper.bridge.connection.transport.ble.impl.api.serial

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import net.flipper.bridge.connection.transport.ble.impl.BleConstants.POLLING_RESET_INTERVAL
import net.flipper.bridge.connection.transport.ble.impl.serial.FResetSerialBleApi
import net.flipper.bridge.connection.transport.ble.impl.toRequestCounter
import net.flipper.bridge.connection.transport.ble.impl.toUInt32ByteArray
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.core.WriteType

class FResetSerialBleApiImpl(
    scope: CoroutineScope,
    resetCharacteristicFlow: Flow<RemoteCharacteristic?>,
) : FResetSerialBleApi, LogTagProvider {
    override val TAG = "FResetSerialBleApi"

    private val forceRecheckSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val characteristicSharedFlow = resetCharacteristicFlow
        .filterNotNull()
        .shareIn(scope, SharingStarted.Eagerly, replay = 1)

    private val requestCounterFlow = characteristicSharedFlow
        .flatMapLatest { characteristic ->
            flow {
                while (currentCoroutineContext().isActive) {
                    val counter = runSuspendCatching { characteristic.read() }
                        .getOrNull()
                        ?.toRequestCounter()
                    debug { "Receive request counter $counter" }
                    emit(counter)
                    withTimeoutOrNull(POLLING_RESET_INTERVAL) {
                        forceRecheckSignal.first()
                    }
                }
            }
        }
        .filterNotNull()
        .shareIn(scope, SharingStarted.Eagerly, 1)

    override fun getRequestCounterFlow(): Flow<Int> {
        return requestCounterFlow
    }

    override suspend fun reset() {
        error { "Reset requested" }
        val characteristic = characteristicSharedFlow.first()
        characteristic.write(0.toUInt32ByteArray(), WriteType.WITH_RESPONSE)
        info { "Characteristic written, waiting for reset..." }
        forceRecheckSignal.tryEmit(Unit)
        requestCounterFlow.filter { it == 0 }.first()
    }
}
