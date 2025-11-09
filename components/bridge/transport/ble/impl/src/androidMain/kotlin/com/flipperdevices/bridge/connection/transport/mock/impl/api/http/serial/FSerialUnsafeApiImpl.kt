package com.flipperdevices.bridge.connection.transport.mock.impl.api.http.serial

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.flipperdevices.bridge.connection.transport.mock.impl.exception.BLEConnectionPermissionException
import com.flipperdevices.core.busylib.log.LogTagProvider
import com.flipperdevices.core.busylib.log.error
import com.flipperdevices.core.busylib.log.info
import com.r0adkll.kimchi.annotations.StringKey
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.AssistedFactory
import me.tatarka.inject.annotations.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.core.CharacteristicProperty
import no.nordicsemi.kotlin.ble.core.WriteType
import no.nordicsemi.kotlin.ble.core.util.chunked

// Max chunk size, limited by firmware:
// https://github.com/flipperdevices/bsb-firmware/blob/d429cf84d9ec7a0160b22726491aca7aef259c8d/applications/system/ble_usart_echo/ble_usart_echo.c#L20
private const val MAX_ATTRIBUTE_SIZE = 237

@Inject
@OptIn(ExperimentalStdlibApi::class)
class FSerialUnsafeApiImpl(
    @Assisted private val rxCharacteristic: Flow<RemoteCharacteristic?>,
    @Assisted private val txCharacteristic: Flow<RemoteCharacteristic?>,
    @Assisted scope: CoroutineScope,
    private val context: Context,
) : LogTagProvider {
    override val TAG = "FSerialUnsafeApiImpl"

    private val receiverByteFlow = MutableSharedFlow<ByteArray>()
    private val isSubscribed = Waiter(false)

    init {
        scope.launch {
            rxCharacteristic
                .filterNotNull()
                .onEach {
                    if (it.isNotifyAvailable().not()) {
                        error { "Found char ${it.uuid}, but notify is disabled" }
                    }
                }
                .filter {
                    it.isNotifyAvailable()
                }
                .flatMapLatest {
                    info { "Start subscribe on rx char" }
                    val flow = it.subscribe()
                    isSubscribed.set(true)
                    return@flatMapLatest flow
                }.collect {
                    info { "Receive data: ${it.decodeToString()}" }
                    receiverByteFlow.emit(it)
                }
        }
    }

    fun getReceiveBytesFlow(): Flow<ByteArray> {
        return receiverByteFlow.asSharedFlow()
    }

    suspend fun sendBytes(data: ByteArray) {
        isSubscribed.waitUntil { it }

        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            throw BLEConnectionPermissionException()
        }
        val writeCharacteristic = txCharacteristic
            .filterNotNull()
            .first()

        data.chunked(MAX_ATTRIBUTE_SIZE).forEach {
            info { "Write chunk with ${it.size} with max size $MAX_ATTRIBUTE_SIZE" }
            writeCharacteristic.write(it, WriteType.WITH_RESPONSE)
        }
    }

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(
            rxCharacteristic: Flow<RemoteCharacteristic?>,
            txCharacteristic: Flow<RemoteCharacteristic?>,
            scope: CoroutineScope
        ): FSerialUnsafeApiImpl
    }
}

private fun RemoteCharacteristic.isNotifyAvailable(): Boolean {
    return properties
        .intersect(listOf(CharacteristicProperty.NOTIFY, CharacteristicProperty.INDICATE))
        .isNotEmpty()
}

private class Waiter<T>(initial: T) {
    private var value: T = initial
    private val channel = Channel<T>()

    suspend fun set(value: T) {
        this.value = value
        channel.trySend(value)
    }

    suspend fun waitUntil(block: (T) -> Boolean) {
        while (!block(value)) {
            if (block(channel.receive())) {
                return
            }
        }
    }
}
