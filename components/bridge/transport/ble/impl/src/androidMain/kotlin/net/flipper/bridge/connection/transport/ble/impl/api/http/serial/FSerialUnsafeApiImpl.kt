package net.flipper.bridge.connection.transport.ble.impl.api.http.serial

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.ble.api.MAX_ATTRIBUTE_SIZE
import net.flipper.bridge.connection.transport.ble.common.exception.BLEConnectionPermissionException
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.core.CharacteristicProperty
import no.nordicsemi.kotlin.ble.core.WriteType
import no.nordicsemi.kotlin.ble.core.util.chunked

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

    @Inject
    class Factory(
        private val factory: (
            Flow<RemoteCharacteristic?>,
            Flow<RemoteCharacteristic?>,
            CoroutineScope
        ) -> FSerialUnsafeApiImpl
    ) {
        operator fun invoke(
            rxCharacteristic: Flow<RemoteCharacteristic?>,
            txCharacteristic: Flow<RemoteCharacteristic?>,
            scope: CoroutineScope
        ): FSerialUnsafeApiImpl = factory(rxCharacteristic, txCharacteristic, scope)
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
