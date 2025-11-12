package net.flipper.transport.ble.impl.cb

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.core.busylib.ktx.common.WrappedSharedFlow
import net.flipper.core.busylib.ktx.common.WrappedStateFlow
import net.flipper.core.busylib.ktx.common.wrap
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.warn
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBPeripheralDelegateProtocol
import platform.CoreBluetooth.CBService
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUUID
import platform.Foundation.dataUsingEncoding
import platform.darwin.NSObject

interface FPeripheralApi {
    val identifier: NSUUID
    val name: String?
    val stateStream: WrappedStateFlow<FPeripheralState>

    val rxDataStream: WrappedSharedFlow<NSData>
    val metaInfoKeysStream: WrappedStateFlow<Map<TransportMetaInfoKey, NSData?>>

    suspend fun writeValue(data: NSData)

    suspend fun onConnecting()
    suspend fun onConnect()
    suspend fun onDisconnecting()
    suspend fun onDisconnect()
    suspend fun onError(error: NSError)
}

@OptIn(ExperimentalForeignApi::class)
@Suppress("TooManyFunctions")
private class FPeripheralDelegate(
    private val peripheral: FPeripheral,
    private val scope: CoroutineScope
) : NSObject(), CBPeripheralDelegateProtocol {

    override fun peripheralDidUpdateName(peripheral: CBPeripheral) {
        this.peripheral.updateName(peripheral)
    }

    override fun peripheral(
        peripheral: CBPeripheral,
        didDiscoverServices: NSError?
    ) {
        this.peripheral.handleDidDiscoverServices(peripheral, didDiscoverServices)
    }

    override fun peripheral(
        peripheral: CBPeripheral,
        didDiscoverCharacteristicsForService: CBService,
        error: NSError?
    ) {
        scope.launch {
            this@FPeripheralDelegate.peripheral.didDiscoverCharacteristics(
                service = didDiscoverCharacteristicsForService,
                characteristics = didDiscoverCharacteristicsForService.characteristics?.map {
                    it as CBCharacteristic
                } ?: emptyList()
            )
        }
    }

    @ObjCSignatureOverride
    override fun peripheral(
        peripheral: CBPeripheral,
        didUpdateValueForCharacteristic: CBCharacteristic,
        error: NSError?
    ) {
        scope.launch {
            this@FPeripheralDelegate.peripheral.didUpdateValue(
                characteristic = didUpdateValueForCharacteristic,
                error = error
            )
        }
    }

    @ObjCSignatureOverride
    override fun peripheral(
        peripheral: CBPeripheral,
        didWriteValueForCharacteristic: CBCharacteristic,
        error: NSError?
    ) {
        this.peripheral.handleDidWriteValue(didWriteValueForCharacteristic, error)
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

    private val _rxDataStream = MutableSharedFlow<NSData>()
    override val rxDataStream: WrappedSharedFlow<NSData> = _rxDataStream.asSharedFlow().wrap()

    private val _metaInfoKeysStream =
        MutableStateFlow<Map<TransportMetaInfoKey, NSData?>>(emptyMap())
    override val metaInfoKeysStream: WrappedStateFlow<Map<TransportMetaInfoKey, NSData?>> =
        _metaInfoKeysStream.asStateFlow().wrap()

    private var serialWrite: CBCharacteristic? = null

    private val delegate = FPeripheralDelegate(this, scope)

    init {
        peripheral.delegate = delegate
        updateName(peripheral)
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
            return
        }

        _stateStream.emit(FPeripheralState.DISCONNECTED)
        debug { "Peripheral onDisconnect id=${identifier.UUIDString}" }
    }

    override suspend fun onError(error: NSError) {
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
        warn { "Peripheral CBError id=${identifier.UUIDString} code=$code" }
    }

    private suspend fun handleCBATTError(code: Long) {
        when (code) {
            15L -> _stateStream.emit(FPeripheralState.PAIRING_FAILED) // CBATTErrorInsufficientEncryption
        }
        warn { "Peripheral CBATTError id=${identifier.UUIDString} code=$code" }
    }

    override suspend fun writeValue(data: NSData) {
        if (stateStream.value != FPeripheralState.CONNECTED) {
            return
        }

        val characteristic = serialWrite ?: return

        debug { "Peripheral writeValue bytes=${data.length} id=${identifier.UUIDString}" }
        peripheral.writeValue(
            data,
            forCharacteristic = characteristic,
            type = 0L // CBCharacteristicWriteWithResponse
        )
    }

    internal fun updateName(peripheral: CBPeripheral) {
        debug { "Peripheral name updated: ${peripheral.name}" }
        val name = peripheral.name ?: return

        val nsName = name as NSString
        val data = nsName.dataUsingEncoding(NSUTF8StringEncoding) ?: return

        _metaInfoKeysStream.update {
            val newMap = it.toMutableMap()
            newMap[TransportMetaInfoKey.DEVICE_NAME] = data
            newMap
        }
    }

    internal fun handleDidDiscoverServices(
        peripheral: CBPeripheral,
        @Suppress("UnusedParameter") didDiscoverServices: NSError?
    ) {
        peripheral.services?.forEach { service ->
            val cbService = service as CBService
            peripheral.discoverCharacteristics(null, forService = cbService)
        }
    }

    internal suspend fun didDiscoverCharacteristics(
        service: CBService,
        characteristics: List<CBCharacteristic>
    ) {
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
                this.serialWrite = serialWrite
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

    internal suspend fun didUpdateValue(characteristic: CBCharacteristic, error: NSError?) {
        val characteristicUUID = characteristic.UUID.toKotlinUUID()
        val data = characteristic.value

        info { "Received value for $characteristicUUID" }

        if (error != null) {
            onError(error)
            return
        }

        // Check if this is serial read data
        if (characteristicUUID == config.serialConfig.rxServiceCharUuid) {
            if (data != null) {
                _rxDataStream.emit(data)
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
            updateMetaInfo(key = metaKey, data = data)
        }
    }

    private fun updateMetaInfo(key: TransportMetaInfoKey, data: NSData?) {
        debug { "Update meta info key=$key" }

        _metaInfoKeysStream.update {
            val newMap = it.toMutableMap()
            newMap[key] = data
            newMap
        }
    }

    internal fun handleDidWriteValue(
        didWriteValueForCharacteristic: CBCharacteristic,
        error: NSError?
    ) {
        if (error != null) {
            this.error { "Write failed: ${error.localizedDescription}" }
        } else {
            debug { "Write succeeded for ${didWriteValueForCharacteristic.UUID.UUIDString}" }
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
