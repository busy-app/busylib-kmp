package com.flipperdevices.transport.ble.impl.cb

import com.flipperdevices.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import com.flipperdevices.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import com.flipperdevices.core.busylib.log.LogTagProvider
import com.flipperdevices.core.busylib.log.debug
import com.flipperdevices.core.busylib.log.error
import com.flipperdevices.core.busylib.log.info
import com.flipperdevices.core.busylib.log.warn
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val stateStream: StateFlow<FPeripheralState>

    val rxDataStream: SharedFlow<NSData>
    val metaInfoKeysStream: StateFlow<Map<TransportMetaInfoKey, NSData?>>

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
    override val stateStream: StateFlow<FPeripheralState> = _stateStream.asStateFlow()

    private val _rxDataStream = MutableSharedFlow<NSData>()
    override val rxDataStream: SharedFlow<NSData> = _rxDataStream.asSharedFlow()

    private val _metaInfoKeysStream =
        MutableStateFlow<Map<TransportMetaInfoKey, NSData?>>(emptyMap())
    override val metaInfoKeysStream: StateFlow<Map<TransportMetaInfoKey, NSData?>> =
        _metaInfoKeysStream.asStateFlow()

    private var serialWrite: CBCharacteristic? = null
    private var statusJob: Job? = null

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
        didDiscoverServices: NSError?
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
        info { "Discovered ${characteristics.size} characteristic(s) under ${service.UUID}" }

        val serviceUuid = service.UUID.UUIDString.lowercase()

        // Check if this is the serial service
        if (serviceUuid == config.serialConfig.serialServiceUuid.toString().lowercase()) {
            val serialRead = characteristics.firstOrNull {
                it.UUID.UUIDString.lowercase() == config.serialConfig.rxServiceCharUuid.toString()
                    .lowercase()
            }
            val serialWrite = characteristics.firstOrNull {
                it.UUID.UUIDString.lowercase() == config.serialConfig.txServiceCharUuid.toString()
                    .lowercase()
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

        // Check for meta info characteristics from config
        val serviceAddress = kotlin.uuid.Uuid.parse(serviceUuid)

        characteristics.forEach { characteristic ->
            val charUuid = characteristic.UUID.UUIDString.lowercase()
            val charAddress = kotlin.uuid.Uuid.parse(charUuid)

            // Find if this characteristic is in our meta info map
            val metaKey = config.metaInfoGattMap.entries.firstOrNull { (_, address) ->
                address.serviceAddress == serviceAddress &&
                    address.characteristicAddress == charAddress
            }?.key

            debug { "Found meta info characteristic: $charUuid for key=$metaKey" }
            if (metaKey == null) {
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
        val uuid = characteristic.UUID.UUIDString.lowercase()
        val data = characteristic.value

        info { "Received value for $uuid" }

        if (error != null) {
            onError(error)
            return
        }

        // Check if this is serial read data
        if (uuid == config.serialConfig.rxServiceCharUuid.toString().lowercase()) {
            if (data != null) {
                _rxDataStream.emit(data)
                debug { "RX data chunk bytes=${data.length} id=${identifier.UUIDString}" }
            }
            return
        }

        val metaKey = fromCBUUID(characteristic.UUID, config)
        if (metaKey != null) {
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

private fun fromCBUUID(
    uuid: CBUUID,
    config: FBleDeviceConnectionConfig
): TransportMetaInfoKey? {
    val uuidString = uuid.UUIDString.lowercase()

    return config.metaInfoGattMap.entries.firstOrNull { (_, address) ->
        address.characteristicAddress.toString().lowercase() == uuidString
    }?.key
}
