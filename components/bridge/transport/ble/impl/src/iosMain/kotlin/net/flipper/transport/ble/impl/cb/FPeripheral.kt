package net.flipper.transport.ble.impl.cb

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.ble.api.MAX_ATTRIBUTE_SIZE
import net.flipper.bridge.connection.transport.ble.api.WRITE_ACK_TIMEOUT_MS
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.busylib.core.wrapper.WrappedSharedFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.withLock
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.warn
import net.flipper.transport.ble.impl.chunked
import net.flipper.transport.ble.impl.toByteArray
import net.flipper.transport.ble.impl.toNSData
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBPeripheralDelegateProtocol
import platform.CoreBluetooth.CBService
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSError
import platform.Foundation.NSUUID
import platform.darwin.NSObject

interface FPeripheralApi {
    val identifier: NSUUID
    val name: String?
    val stateStream: WrappedStateFlow<FPeripheralState>

    val rxDataStream: WrappedSharedFlow<ByteArray>
    val metaInfoKeysStream: WrappedStateFlow<Map<TransportMetaInfoKey, ByteArray?>>

    suspend fun writeValue(data: ByteArray)

    suspend fun onConnecting()
    suspend fun onConnect()
    suspend fun onDisconnecting()
    suspend fun onDisconnect()
    suspend fun onError(error: NSError)
}

@OptIn(ExperimentalForeignApi::class)
@Suppress("TooManyFunctions")
private class FPeripheralDelegate(
    val didDiscoverServices: (CBPeripheral, NSError?) -> Unit,
    val didDiscoverCharacteristics: (CBService, NSError?) -> Unit,
    val didUpdateValueForCharacteristic: (CBCharacteristic, NSError?) -> Unit,
    val didWriteValueForCharacteristic: (CBCharacteristic, NSError?) -> Unit,
) : NSObject(), CBPeripheralDelegateProtocol {

    override fun peripheral(
        peripheral: CBPeripheral,
        didDiscoverServices: NSError?
    ) {
        didDiscoverServices(peripheral, didDiscoverServices)
    }

    override fun peripheral(
        peripheral: CBPeripheral,
        didDiscoverCharacteristicsForService: CBService,
        error: NSError?
    ) {
        didDiscoverCharacteristics(didDiscoverCharacteristicsForService, error)
    }

    @ObjCSignatureOverride
    override fun peripheral(
        peripheral: CBPeripheral,
        didUpdateValueForCharacteristic: CBCharacteristic,
        error: NSError?
    ) {
        didUpdateValueForCharacteristic(didUpdateValueForCharacteristic, error)
    }

    @ObjCSignatureOverride
    override fun peripheral(
        peripheral: CBPeripheral,
        didWriteValueForCharacteristic: CBCharacteristic,
        error: NSError?
    ) {
        didWriteValueForCharacteristic(didWriteValueForCharacteristic, error)
    }
}

@OptIn(ExperimentalForeignApi::class)
@Suppress("TooManyFunctions")
class FPeripheral(
    private val peripheral: CBPeripheral,
    private val config: FBleDeviceConnectionConfig,
    private val scope: CoroutineScope
) : FPeripheralApi, LogTagProvider {

    override val TAG: String = "FPeripheral"

    override val identifier: NSUUID
        get() = peripheral.identifier
    override val name: String?
        get() = peripheral.name

    private val _stateStream = MutableStateFlow(FPeripheralState.from(peripheral.state))
    override val stateStream: WrappedStateFlow<FPeripheralState> = _stateStream.asStateFlow().wrap()

    private val _rxDataStream = MutableSharedFlow<ByteArray>()
    override val rxDataStream: WrappedSharedFlow<ByteArray> = _rxDataStream.asSharedFlow().wrap()

    private val _metaInfoKeysStream =
        MutableStateFlow<Map<TransportMetaInfoKey, ByteArray?>>(emptyMap())
    override val metaInfoKeysStream: WrappedStateFlow<Map<TransportMetaInfoKey, ByteArray?>> =
        _metaInfoKeysStream.asStateFlow().wrap()

    private var serialWrite: CBCharacteristic? = null
    private val writeMutex = Mutex()
    private var writeCompletionDeferred: CompletableDeferred<Unit>? = null

    private val delegate = FPeripheralDelegate(
        didDiscoverServices = { peripheral, error ->
            scope.launch { handleDidDiscoverServices(peripheral, error) }
        },
        didDiscoverCharacteristics = { service, error ->
            scope.launch { didDiscoverCharacteristics(service, error) }
        },
        didUpdateValueForCharacteristic = { characteristic, error ->
            scope.launch { didUpdateValue(characteristic, error) }
        },
        didWriteValueForCharacteristic = { characteristic, error ->
            scope.launch { handleDidWriteValue(characteristic, error) }
        },
    )

    init {
        peripheral.delegate = delegate
    }

    override suspend fun onConnecting() {
        _stateStream.emit(FPeripheralState.CONNECTING)
        debug { "Peripheral onConnecting id=${identifier.UUIDString}" }
    }

    override suspend fun onConnect() {
        debug { "Peripheral onConnect — discovering services id=${identifier.UUIDString}" }
        peripheral.discoverServices(null)
    }

    override suspend fun onDisconnecting() {
        _stateStream.emit(FPeripheralState.DISCONNECTING)
        debug { "Peripheral onDisconnecting id=${identifier.UUIDString}" }
    }

    override suspend fun onDisconnect() {
        if (stateStream.value == FPeripheralState.PAIRING_FAILED ||
            stateStream.value == FPeripheralState.INVALID_PAIRING
        ) {
            debug { "#onDisconnect by reason PAIRING_FAILED or INVALID_PAIRING" }
            return
        }

        writeCompletionDeferred?.completeExceptionally(
            kotlinx.coroutines.CancellationException("Disconnected")
        )
        writeCompletionDeferred = null

        serialWrite = null
        _metaInfoKeysStream.emit(emptyMap())
        _stateStream.emit(FPeripheralState.DISCONNECTED)
        debug { "Peripheral onDisconnect id=${identifier.UUIDString}" }
    }

    override suspend fun onError(error: NSError) {
        error { "#onError ${error.localizedDescription}" }
        val domain = error.domain
        val code = error.code

        when (domain) {
            "CBATTErrorDomain" -> handleCBATTError(code)
            "CBErrorDomain" -> handleCBError(code)
        }
    }

    private suspend fun handleCBError(code: Long) {
        when (code) {
            7L -> _stateStream.emit(FPeripheralState.INVALID_PAIRING) // CBErrorPeerRemovedPairingInformation
            17L -> _stateStream.emit(FPeripheralState.DISCONNECTED) // CBErrorEncryptionTimedOut
        }
        error { "Peripheral CBError id=${identifier.UUIDString} code=$code" }
    }

    private suspend fun handleCBATTError(code: Long) {
        when (code) {
            15L -> _stateStream.emit(FPeripheralState.PAIRING_FAILED) // CBATTErrorInsufficientEncryption
        }
        error { "Peripheral CBATTError id=${identifier.UUIDString} code=$code" }
    }

    override suspend fun writeValue(data: ByteArray) {
        if (stateStream.value != FPeripheralState.CONNECTED) {
            warn { "#writeValue cannot write because state not connected" }
            return
        }
        val characteristic = serialWrite
        if (characteristic == null) {
            warn { "#writeValue cannot write because serialWrite characteristic is null" }
            return
        }
        debug { "Peripheral writeValue bytes=${data.size} id=${identifier.UUIDString}" }
        withLock(writeMutex, "writeValue") {
            data.chunked(MAX_ATTRIBUTE_SIZE).forEach { chunk ->
                if (stateStream.value != FPeripheralState.CONNECTED) {
                    warn { "#writeValue aborting — disconnected during chunked write" }
                    return@withLock
                }
                debug { "Write chunk with ${chunk.size} with max size $MAX_ATTRIBUTE_SIZE" }

                val deferred = CompletableDeferred<Unit>()
                writeCompletionDeferred = deferred

                peripheral.writeValue(
                    chunk.toNSData(),
                    forCharacteristic = characteristic,
                    type = 0L // CBCharacteristicWriteWithResponse
                )

                withTimeout(WRITE_ACK_TIMEOUT_MS) {
                    deferred.await()
                }
            }
        }
    }

    internal suspend fun handleDidDiscoverServices(
        peripheral: CBPeripheral,
        didDiscoverServices: NSError?
    ) {
        if (didDiscoverServices != null) {
            error { "Service discovery failed id=${identifier.UUIDString} error=$didDiscoverServices" }
            return
        }

        peripheral.services?.forEach { service ->
            val cbService = service as CBService
            peripheral.discoverCharacteristics(null, forService = cbService)
        }
    }

    internal suspend fun didDiscoverCharacteristics(
        service: CBService,
        error: NSError?
    ) {
        if (error != null) {
            error { "Characteristic discovery failed id=${identifier.UUIDString} error=$error" }
            return
        }

        val characteristics = service.characteristics?.map { it as CBCharacteristic } ?: emptyList()

        val serviceUUID = service.UUID.toKotlinUUID()
        info { "Service $service UUID ${service.UUID} Kotlin $serviceUUID" }

        if (serviceUUID == config.serialConfig.serialServiceUuid) {
            val serialRead = characteristics.firstOrNull {
                it.UUID.toKotlinUUID() == config.serialConfig.rxServiceCharUuid
            }
            val serialWrite = characteristics.firstOrNull {
                it.UUID.toKotlinUUID() == config.serialConfig.txServiceCharUuid
            }

            if (serialRead != null && serialWrite != null) {
                peripheral.setNotifyValue(true, forCharacteristic = serialRead)
                this@FPeripheral.serialWrite = serialWrite
                info { "Serial characteristics ready (read/write) id=${identifier.UUIDString}" }
            } else {
                error { "Serial characteristics not found id=${identifier.UUIDString}" }
            }
            return
        }

        characteristics.forEach { characteristic ->
            val characteristicUUID = characteristic.UUID.toKotlinUUID()
            info { "Characteristic UUID: $characteristicUUID" }

            val metaKey = config.metaInfoGattMap.entries.firstOrNull { (_, address) ->
                address.characteristicAddress == characteristicUUID
            }?.key

            if (metaKey == null) {
                warn { "Unknown characteristic discovered: $characteristicUUID" }
                return@forEach
            }

            if (
                metaKey == TransportMetaInfoKey.BATTERY_LEVEL ||
                metaKey == TransportMetaInfoKey.BATTERY_POWER_STATE
            ) {
                peripheral.setNotifyValue(true, forCharacteristic = characteristic)
                debug { "Subscribed to $metaKey characteristic" }
            }

            peripheral.readValueForCharacteristic(characteristic)
            debug { "Reading meta info characteristic: $metaKey" }
        }
    }

    internal suspend fun didUpdateValue(
        characteristic: CBCharacteristic,
        error: NSError?
    ) {
        val characteristicUUID = characteristic.UUID.toKotlinUUID()
        val data = characteristic.value

        debug { "Received value for $characteristicUUID" }

        if (error != null) {
            error { "#didUpdateValue failed ${error.localizedDescription}" }
            onError(error)
            return
        }

        // Check if this is serial read data
        if (characteristicUUID == config.serialConfig.rxServiceCharUuid) {
            if (data != null) {
                _rxDataStream.emit(data.toByteArray())
                debug { "RX data chunk bytes=${data.length} id=${identifier.UUIDString}" }
            } else {
                warn { "RX data is null id=${identifier.UUIDString}" }
            }
            return
        }

        val metaKey = config.metaInfoGattMap.entries.firstOrNull { (_, address) ->
            address.characteristicAddress == characteristicUUID
        }?.key

        if (metaKey == null) {
            warn { "Unknown characteristic updated: $characteristicUUID" }
        } else {
            _stateStream.emit(FPeripheralState.CONNECTED)
            updateMetaInfo(key = metaKey, data = data?.toByteArray())
        }
    }

    private fun updateMetaInfo(key: TransportMetaInfoKey, data: ByteArray?) {
        debug { "Update meta info key=$key content ${data?.decodeToString()}" }

        _metaInfoKeysStream.update {
            val newMap = it.toMutableMap()
            newMap[key] = data
            newMap
        }
    }

    internal suspend fun handleDidWriteValue(
        didWriteValueForCharacteristic: CBCharacteristic,
        error: NSError?
    ) {
        val writeCharacteristic = serialWrite ?: return
        if (didWriteValueForCharacteristic.UUID.toKotlinUUID() == writeCharacteristic.UUID.toKotlinUUID()) {
            val deferred = writeCompletionDeferred ?: return
            writeCompletionDeferred = null
            if (error != null) {
                deferred.completeExceptionally(Exception(error.localizedDescription))
            } else {
                deferred.complete(Unit)
            }
        }
    }
}

private fun CBUUID.toKotlinUUID(): kotlin.uuid.Uuid {
    /**
     * Convert short-form Bluetooth SIG UUIDs to full 128-bit format.
     * Standard Bluetooth UUIDs use the base: 0000XXXX-0000-1000-8000-00805F9B34FB
     * where XXXX is the short form (16-bit or 32-bit).
     */
    fun normalizeUuid(uuidString: String): String {
        val lowercase = uuidString.lowercase()

        // If it already has dashes and is 36 characters, it's already a full UUID
        if (lowercase.contains("-") && lowercase.length == 36) {
            return lowercase
        }

        // Remove dashes for processing
        val withoutDashes = lowercase.replace("-", "")

        // Already a full UUID without dashes (32 hex chars)
        if (withoutDashes.length == 32) {
            // Format as standard UUID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
            return "${withoutDashes.substring(0, 8)}-${withoutDashes.substring(8, 12)}-" +
                "${withoutDashes.substring(12, 16)}-${withoutDashes.substring(16, 20)}-" +
                withoutDashes.substring(20, 32)
        }

        // Short form UUID (4 or 8 characters) - convert to full Bluetooth SIG UUID
        if (withoutDashes.length == 4 || withoutDashes.length == 8) {
            val paddedUuid = withoutDashes.padStart(4, '0')
            return "0000$paddedUuid-0000-1000-8000-00805f9b34fb"
        }

        // Unknown format - try to parse as hex and convert to Bluetooth SIG format
        // This handles malformed UUIDs by extracting just the service ID part
        warn { "Unexpected UUID format: $uuidString (length=${withoutDashes.length}) " }

        // Try to extract first 4 hex characters as the service ID
        val hexDigits = withoutDashes.filter { it in "0123456789abcdef" }
        val serviceId = when {
            hexDigits.length >= 4 -> hexDigits.substring(0, 4).padStart(4, '0')
            else -> {
                error { "Unable to normalize UUID: $uuidString" }
                return lowercase // Return as-is as last resort
            }
        }

        return "0000$serviceId-0000-1000-8000-00805f9b34fb"
    }

    val uuid = normalizeUuid(this.UUIDString)
    return kotlin.uuid.Uuid.parse(uuid)
}
