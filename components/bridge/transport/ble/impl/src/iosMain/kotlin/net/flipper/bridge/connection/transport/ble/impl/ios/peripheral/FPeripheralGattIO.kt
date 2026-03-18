package net.flipper.bridge.connection.transport.ble.impl.ios.peripheral

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import net.flipper.bridge.connection.transport.ble.api.MAX_ATTRIBUTE_SIZE
import net.flipper.bridge.connection.transport.ble.api.WRITE_ACK_TIMEOUT_MS
import net.flipper.core.busylib.ktx.common.chunked
import net.flipper.core.busylib.ktx.common.toNSData
import net.flipper.core.busylib.ktx.common.withLock
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.warn
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBPeripheral
import platform.Foundation.NSError
import kotlin.uuid.Uuid

internal class FPeripheralGattIO(
    private val peripheral: CBPeripheral,
    private val stateProvider: () -> FPeripheralState,
    private val serialWriteProvider: () -> CBCharacteristic?,
    private val characteristicProvider: (Uuid) -> CBCharacteristic?,
) : LogTagProvider {

    override val TAG: String = "FPeripheralGattIO"

    private val writeMutex = Mutex()
    private val writeDeferreds = mutableMapOf<Uuid, CompletableDeferred<Unit>>()
    private val readDeferreds = mutableMapOf<Uuid, CompletableDeferred<ByteArray>>()

    suspend fun writeValue(data: ByteArray) {
        if (stateProvider() != FPeripheralState.CONNECTED) {
            warn { "#writeValue cannot write because state not connected" }
            return
        }
        val characteristic = serialWriteProvider()
        if (characteristic == null) {
            warn { "#writeValue cannot write because serialWrite characteristic is null" }
            return
        }
        val serialWriteUuid = characteristic.UUID.toKotlinUUID()
        debug { "Peripheral writeValue bytes=${data.size} id=${peripheral.identifier.UUIDString}" }
        withLock(writeMutex, "writeValue") {
            data.chunked(MAX_ATTRIBUTE_SIZE).forEach { chunk ->
                if (stateProvider() != FPeripheralState.CONNECTED) {
                    warn { "#writeValue aborting — disconnected during chunked write" }
                    return@withLock
                }
                debug { "Write chunk with ${chunk.size} with max size $MAX_ATTRIBUTE_SIZE" }

                val deferred = CompletableDeferred<Unit>()
                writeDeferreds[serialWriteUuid]?.completeExceptionally(
                    CancellationException("Superseded by newer serial write")
                )
                writeDeferreds[serialWriteUuid] = deferred

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
                    if (writeDeferreds[serialWriteUuid] === deferred) {
                        writeDeferreds.remove(serialWriteUuid)
                    }
                }
            }
        }
    }

    suspend fun readValue(characteristicUuid: Uuid): ByteArray {
        checkConnected("readValue")
        val characteristic = characteristicProvider(characteristicUuid)
            ?: error("Characteristic $characteristicUuid not found")

        val deferred = CompletableDeferred<ByteArray>()
        readDeferreds[characteristicUuid]?.completeExceptionally(
            CancellationException("Superseded by newer read request")
        )
        readDeferreds[characteristicUuid] = deferred

        peripheral.readValueForCharacteristic(characteristic)

        return try {
            withTimeout(WRITE_ACK_TIMEOUT_MS) {
                deferred.await()
            }
        } finally {
            if (readDeferreds[characteristicUuid] === deferred) {
                readDeferreds.remove(characteristicUuid)
            }
        }
    }

    suspend fun writeValue(characteristicUuid: Uuid, data: ByteArray) {
        checkConnected("writeValue")
        val characteristic = characteristicProvider(characteristicUuid)
        if (characteristic == null) {
            error { "Characteristic $characteristicUuid not found" }
            return
        }

        withLock(writeMutex, "writeValue[$characteristicUuid]") {
            val deferred = CompletableDeferred<Unit>()
            writeDeferreds[characteristicUuid]?.completeExceptionally(
                CancellationException("Superseded by newer write request")
            )
            writeDeferreds[characteristicUuid] = deferred

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
                if (writeDeferreds[characteristicUuid] === deferred) {
                    writeDeferreds.remove(characteristicUuid)
                }
            }
        }
    }

    fun completeRead(characteristicUuid: Uuid, payload: ByteArray) {
        val waiter = readDeferreds.remove(characteristicUuid)
        debug {
            "Completing read uuid=$characteristicUuid bytes=${payload.size} " +
                "waiterFound=${waiter != null}"
        }
        waiter?.complete(payload)
    }

    fun failRead(characteristicUuid: Uuid, error: NSError) {
        readDeferreds.remove(characteristicUuid)?.completeExceptionally(Exception(error.localizedDescription))
    }

    fun handleDidWriteValue(
        didWriteValueForCharacteristic: CBCharacteristic,
        error: NSError?
    ) {
        val characteristicUuid = didWriteValueForCharacteristic.UUID.toKotlinUUID()
        val deferred = writeDeferreds.remove(characteristicUuid) ?: return
        if (error != null) {
            deferred.completeExceptionally(Exception(error.localizedDescription))
        } else {
            deferred.complete(Unit)
        }
    }

    fun cancelPending(disconnectException: CancellationException) {
        writeDeferreds.values.forEach { deferred ->
            deferred.completeExceptionally(disconnectException)
        }
        readDeferreds.values.forEach { deferred ->
            deferred.completeExceptionally(disconnectException)
        }
        writeDeferreds.clear()
        readDeferreds.clear()
    }

    private fun checkConnected(operationName: String) {
        check(stateProvider() == FPeripheralState.CONNECTED) {
            "$operationName cannot proceed while state=${stateProvider()}"
        }
    }
}
