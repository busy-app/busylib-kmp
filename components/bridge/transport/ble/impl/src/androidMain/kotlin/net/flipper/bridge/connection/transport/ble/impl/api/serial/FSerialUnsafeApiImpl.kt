package net.flipper.bridge.connection.transport.ble.impl.api.serial

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
import net.flipper.bridge.connection.transport.ble.impl.api.utils.isNotifyAvailable
import net.flipper.bridge.connection.transport.ble.impl.exception.BLEConnectionPermissionException
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.exception.OperationFailedException
import no.nordicsemi.kotlin.ble.core.CharacteristicProperty
import no.nordicsemi.kotlin.ble.core.OperationStatus
import no.nordicsemi.kotlin.ble.core.WriteType
import no.nordicsemi.kotlin.ble.core.util.chunked
import kotlin.time.Duration.Companion.milliseconds

@Inject
@OptIn(ExperimentalStdlibApi::class)
class FSerialUnsafeApiImpl(
    @Assisted private val rxCharacteristic: Flow<RemoteCharacteristic?>,
    @Assisted private val txCharacteristic: Flow<RemoteCharacteristic?>,
    @Assisted scope: CoroutineScope,
    @Assisted private val onResetServices: suspend () -> Unit,
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
                .flatMapLatest { remoteChar ->
                    info { "Start subscribe on rx char" }
                    // onSubscription fires after setNotifying(true) (the CCCD write)
                    // returns. Only then is it safe to issue writes via sendBytes.
                    remoteChar.subscribe(
                        onSubscription = {
                            info { "RX char subscribed, waiting $POST_SUBSCRIBE_SETTLE_DELAY ms delay..." }
                            // Android's bond-triggered service rediscovery can still be
                            // in flight when subscribe() returns. Sit out a short settle
                            // window before allowing the first write so internal stack
                            // ops don't poison our write callback.
                            scope.launch {
                                delay(POST_SUBSCRIBE_SETTLE_DELAY)
                                info { "Post-subscribe settle window elapsed" }
                                isSubscribed.set(true)
                            }
                        }
                    )
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
        data.chunked(MAX_ATTRIBUTE_SIZE).forEach { chunk ->
            debug { "Write chunk with ${chunk.size} with max size $MAX_ATTRIBUTE_SIZE" }
            debug { "Write ${data.decodeToString()}" }
            writeChunkWithRetry(chunk)
            debug { "Return back from write" }
        }
    }

    private suspend fun writeChunkWithRetry(chunk: ByteArray) {
        repeat(MAX_WRITE_ATTEMPTS - 1) { attempt ->
            try {
                val writeCharacteristic = txCharacteristic
                    .filterNotNull()
                    .filter { it.properties.contains(CharacteristicProperty.WRITE) }
                    .first()
                debug { "TX char properties: ${writeCharacteristic.properties}" }
                writeCharacteristic.write(chunk, WriteType.WITH_RESPONSE)
                return
            } catch (e: OperationFailedException) {
                if (e.reason == OperationStatus.AttributeNotFound || e.reason == OperationStatus.GattError) {
                    error(e) {
                        "Write failed on attempt ${attempt + 1}/" +
                            "$MAX_WRITE_ATTEMPTS, retrying after $RETRY_DELAY"
                    }
                    onResetServices()
                    delay(RETRY_DELAY)
                } else {
                    throw e
                }
            }
        }
        val writeCharacteristic = txCharacteristic
            .filterNotNull()
            .filter { it.properties.contains(CharacteristicProperty.WRITE) }
            .first()
        debug { "TX char properties: ${writeCharacteristic.properties}" }
        writeCharacteristic.write(chunk, WriteType.WITH_RESPONSE)
    }

    private companion object {
        const val MAX_WRITE_ATTEMPTS = 3
        val RETRY_DELAY = 200.milliseconds
        val POST_SUBSCRIBE_SETTLE_DELAY = 500.milliseconds
    }

    @Inject
    class Factory(
        private val factory: (
            Flow<RemoteCharacteristic?>,
            Flow<RemoteCharacteristic?>,
            CoroutineScope,
            onResetServices: suspend () -> Unit
        ) -> FSerialUnsafeApiImpl
    ) {
        operator fun invoke(
            rxCharacteristic: Flow<RemoteCharacteristic?>,
            txCharacteristic: Flow<RemoteCharacteristic?>,
            scope: CoroutineScope,
            onResetServices: suspend () -> Unit
        ): FSerialUnsafeApiImpl = factory(rxCharacteristic, txCharacteristic, scope, onResetServices)
    }
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
