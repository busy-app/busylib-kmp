package net.flipper.bridge.connection.transport.ble.impl.ios.peripheral

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import net.flipper.bridge.connection.transport.ble.api.MAX_ATTRIBUTE_SIZE
import net.flipper.bridge.connection.transport.ble.api.WRITE_ACK_TIMEOUT_MS
import net.flipper.core.busylib.ktx.common.chunked
import net.flipper.core.busylib.ktx.common.launchWithLock
import net.flipper.core.busylib.ktx.common.toNSData
import net.flipper.core.busylib.ktx.common.withLock
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBPeripheral
import platform.Foundation.NSError
import kotlin.uuid.Uuid

internal class FPeripheralGattIO(
    private val peripheral: CBPeripheral,
    private val scope: CoroutineScope,
    private val peripheralState: StateFlow<FPeripheralState>,
    private val serialWriteState: StateFlow<CBCharacteristic?>,
    private val characteristicProvider: suspend (Uuid) -> CBCharacteristic,
) : LogTagProvider {

    override val TAG: String = "FPeripheralGattIO"

    private val writeMutex = Mutex()

    private val writeDeferredMutex = Mutex()
    private val writeDeferreds = mutableMapOf<Uuid, CompletableDeferred<Unit>>()
    private val readDeferredMutex = Mutex()
    private val readDeferreds = mutableMapOf<Uuid, CompletableDeferred<ByteArray>>()

    suspend fun writeSerial(data: ByteArray) {
        waitConnected()
        // Wait for serial write state
        val characteristic = serialWriteState.filterNotNull().first()
        val serialWriteUuid = characteristic.UUID.toKotlinUUID()
        debug { "Peripheral writeValue bytes=${data.size} id=${peripheral.identifier.UUIDString}" }
        withLock(writeMutex, "writeValue") {
            data.chunked(MAX_ATTRIBUTE_SIZE).forEach { chunk ->
                if (peripheralState.value != FPeripheralState.CONNECTED) {
                    error("#writeValue aborting — disconnected during chunked write")
                }
                debug { "Write chunk with ${chunk.size} with max size $MAX_ATTRIBUTE_SIZE" }

                val deferred = CompletableDeferred<Unit>()
                withLock(writeDeferredMutex, "write_serial") {
                    writeDeferreds[serialWriteUuid]?.completeExceptionally(
                        CancellationException("Superseded by newer serial write")
                    )
                    writeDeferreds[serialWriteUuid] = deferred
                }
                peripheral.writeValue(
                    chunk.toNSData(),
                    forCharacteristic = characteristic,
                    type = 0L // CBCharacteristicWriteWithResponse
                )

                try {
                    withTimeout(WRITE_ACK_TIMEOUT_MS) {
                        deferred.await()
                    }
                } finally {
                    withLock(writeDeferredMutex, "write_clean") {
                        if (writeDeferreds[serialWriteUuid] === deferred) {
                            writeDeferreds.remove(serialWriteUuid)
                        }
                    }
                }
            }
        }
    }

    suspend fun readValue(characteristicUuid: Uuid): ByteArray {
        waitConnected()
        val characteristic = characteristicProvider(characteristicUuid)
        val deferred = CompletableDeferred<ByteArray>()
        withLock(readDeferredMutex, "read_value") {
            readDeferreds[characteristicUuid]?.completeExceptionally(
                CancellationException("Superseded by newer read request")
            )
            readDeferreds[characteristicUuid] = deferred
        }

        peripheral.readValueForCharacteristic(characteristic)

        return try {
            withTimeout(WRITE_ACK_TIMEOUT_MS) {
                deferred.await()
            }
        } finally {
            withLock(readDeferredMutex, "read_value_final") {
                if (readDeferreds[characteristicUuid] === deferred) {
                    readDeferreds.remove(characteristicUuid)
                }
            }
        }
    }

    suspend fun writeValue(characteristicUuid: Uuid, data: ByteArray) {
        waitConnected()
        val characteristic = characteristicProvider(characteristicUuid)

        val deferred = CompletableDeferred<Unit>()
        withLock(writeDeferredMutex, "write_simple") {
            writeDeferreds[characteristicUuid]?.completeExceptionally(
                CancellationException("Superseded by newer write request")
            )
            writeDeferreds[characteristicUuid] = deferred
        }

        peripheral.writeValue(
            data.toNSData(),
            forCharacteristic = characteristic,
            type = 0L // CBCharacteristicWriteWithResponse
        )

        try {
            withTimeout(WRITE_ACK_TIMEOUT_MS) {
                deferred.await()
            }
        } finally {
            withLock(writeDeferredMutex, "write_simple_final") {
                if (writeDeferreds[characteristicUuid] === deferred) {
                    writeDeferreds.remove(characteristicUuid)
                }
            }
        }
    }

    fun completeRead(characteristicUuid: Uuid, payload: ByteArray) {
        launchWithLock(readDeferredMutex, scope, "complete_read") {
            val waiter = readDeferreds.remove(characteristicUuid)
            debug {
                "Completing read uuid=$characteristicUuid bytes=${payload.size} " +
                    "waiterFound=${waiter != null}"
            }
            waiter?.complete(payload)
        }
    }

    fun failRead(characteristicUuid: Uuid, error: NSError) {
        launchWithLock(readDeferredMutex, scope, "fail_read") {
            readDeferreds.remove(characteristicUuid)
                ?.completeExceptionally(Exception(error.localizedDescription))
        }
    }

    fun handleDidWriteValue(
        didWriteValueForCharacteristic: CBCharacteristic,
        error: NSError?
    ) {
        launchWithLock(writeDeferredMutex, scope, "handle_write") {
            val characteristicUuid = didWriteValueForCharacteristic.UUID.toKotlinUUID()
            val deferred = writeDeferreds.remove(characteristicUuid) ?: return@launchWithLock
            if (error != null) {
                deferred.completeExceptionally(Exception(error.localizedDescription))
            } else {
                deferred.complete(Unit)
            }
        }
    }

    fun cancelPending(disconnectException: CancellationException) {
        launchWithLock(writeDeferredMutex, scope, "cancel_pending_write") {
            writeDeferreds.values.forEach { deferred ->
                deferred.completeExceptionally(disconnectException)
            }
            writeDeferreds.clear()
        }
        launchWithLock(readDeferredMutex, scope, "cancel_pending_read") {
            readDeferreds.values.forEach { deferred ->
                deferred.completeExceptionally(disconnectException)
            }
            readDeferreds.clear()
        }
    }

    private suspend fun waitConnected() {
        // Wait for connected state
        peripheralState
            .filter { it == FPeripheralState.CONNECTED }
            .first()
    }
}
