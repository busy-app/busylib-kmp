package net.flipper.bridge.connection.transport.ble.impl.api.http.serial

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.plus
import net.flipper.bridge.connection.transport.ble.impl.serial.FResetSerialBleApi
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.core.WriteType
import kotlin.time.Duration.Companion.seconds

private val POLLING_INTERVAL = 5.seconds

class FResetSerialBleApiImpl(
    scope: CoroutineScope,
    resetCharacteristicFlow: Flow<RemoteCharacteristic?>,
) : FResetSerialBleApi, LogTagProvider {
    override val TAG = "FResetSerialBleApi"
    private val _requestCounterStateFlow = MutableStateFlow(0)

    private val characteristicSharedFlow = resetCharacteristicFlow
        .filterNotNull()
        .shareIn(scope, SharingStarted.Eagerly, replay = 1)

    init {
        characteristicSharedFlow
            .flatMapLatest { characteristic ->
                flow {
                    while (currentCoroutineContext().isActive) {
                        val counter = characteristic.read().toRequestCounter()
                        emit(counter)
                        delay(POLLING_INTERVAL)
                    }
                }
            }
            .onEach { _requestCounterStateFlow.value = it }
            .launchIn(scope + FlipperDispatchers.default)
    }

    override fun getRequestCounterStateFlow(): StateFlow<Int> {
        return _requestCounterStateFlow.asStateFlow()
    }

    override suspend fun reset() {
        error { "Reset requested" }
        val characteristic = characteristicSharedFlow.first()
        characteristic.write(0.toUInt32ByteArray(), WriteType.WITH_RESPONSE)
        info { "Characteristic written, waiting for reset..." }
        _requestCounterStateFlow.filter { it == 0 }.first()
    }
}

private fun ByteArray.toRequestCounter(): Int {
    if (size < Int.SIZE_BYTES) return 0
    return (this[0].toInt() and 0xFF) or
        ((this[1].toInt() and 0xFF) shl 8) or
        ((this[2].toInt() and 0xFF) shl 16) or
        ((this[3].toInt() and 0xFF) shl 24)
}

private fun Int.toUInt32ByteArray(): ByteArray {
    return byteArrayOf(
        (this and 0xFF).toByte(),
        ((this shr 8) and 0xFF).toByte(),
        ((this shr 16) and 0xFF).toByte(),
        ((this shr 24) and 0xFF).toByte()
    )
}
