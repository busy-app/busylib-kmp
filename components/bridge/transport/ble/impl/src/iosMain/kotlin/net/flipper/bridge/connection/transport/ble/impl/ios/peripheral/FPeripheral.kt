package net.flipper.bridge.connection.transport.ble.impl.ios.peripheral

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import platform.CoreBluetooth.CBATTErrorInsufficientEncryption
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBErrorEncryptionTimedOut
import platform.CoreBluetooth.CBErrorPeerRemovedPairingInformation
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBService
import platform.Foundation.NSError
import platform.Foundation.NSUUID
import kotlin.uuid.Uuid

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

    @Suppress("MagicNumber")
    private val _rxDataChannel = Channel<ByteArray>(2048)
    override val rxDataStream: Flow<ByteArray> = _rxDataChannel.receiveAsFlow()

    @Suppress("MagicNumber")
    private val _streamingDataChannel = Channel<ByteArray>(2048)
    override val streamingDataStream: Flow<ByteArray> = _streamingDataChannel.receiveAsFlow()

    private val _metaInfoKeysStream =
        MutableStateFlow<Map<TransportMetaInfoKey, ByteArray?>>(emptyMap())
    override val metaInfoKeysStream: WrappedStateFlow<Map<TransportMetaInfoKey, ByteArray?>> =
        _metaInfoKeysStream.asStateFlow().wrap()

    private var serialWrite = MutableStateFlow<CBCharacteristic?>(null)
    private val characteristicsByUuid = MutableStateFlow(mapOf<Uuid, CBCharacteristic>())
    private val characteristicValueState = MutableStateFlow<Map<Uuid, ByteArray?>>(emptyMap())
    private val gattIO = FPeripheralGattIO(
        peripheral = peripheral,
        scope = scope,
        peripheralState = _stateStream,
        serialWriteState = serialWrite,
        characteristicProvider = { uuid ->
            characteristicsByUuid.map { it[uuid] }.filterNotNull().first()
        },
    )
    private val discovery = FPeripheralDiscovery(
        config = config,
        characteristicsByUuid = characteristicsByUuid,
        serialWriteUpdater = { characteristic -> serialWrite.value = characteristic },
        identifierProvider = { identifier.UUIDString },
    )
    private val valueRouter = FPeripheralValueRouter(
        config = config,
        stateStream = _stateStream,
        rxDataChannel = _rxDataChannel,
        streamingDataChannel = _streamingDataChannel,
        metaInfoKeysStream = _metaInfoKeysStream,
        characteristicValueState = characteristicValueState,
        gattIO = gattIO,
        onError = { callbackError -> onError(callbackError) },
        identifierProvider = { identifier.UUIDString },
    )

    private val delegate = FPeripheralDelegate(
        didDiscoverServices = { peripheral, error ->
            handleDidDiscoverServices(peripheral, error)
        },
        didDiscoverCharacteristics = { service, error ->
            didDiscoverCharacteristics(service, error)
        },
        didUpdateValueForCharacteristic = { characteristic, error ->
            didUpdateValue(characteristic, error)
        },
        didWriteValueForCharacteristic = { characteristic, error ->
            handleDidWriteValue(characteristic, error)
        },
    )

    init {
        peripheral.delegate = delegate
    }

    override suspend fun onConnecting() {
        debug { "Peripheral onConnecting id=${identifier.UUIDString}" }
        _stateStream.emit(FPeripheralState.CONNECTING)
    }

    override suspend fun onConnect() {
        debug { "Peripheral onConnect — discovering services id=${identifier.UUIDString}" }
        peripheral.discoverServices(null)
    }

    override suspend fun onDisconnecting() {
        debug { "Peripheral onDisconnecting id=${identifier.UUIDString}" }
        _stateStream.emit(FPeripheralState.DISCONNECTING)
    }

    override suspend fun onDisconnect() {
        debug { "Peripheral onDisconnect id=${identifier.UUIDString}" }

        if (stateStream.value == FPeripheralState.PAIRING_FAILED ||
            stateStream.value == FPeripheralState.INVALID_PAIRING
        ) {
            debug { "#onDisconnect by reason PAIRING_FAILED or INVALID_PAIRING" }
            return
        }

        val disconnectException = CancellationException("Disconnected")
        gattIO.cancelPending(disconnectException)

        _rxDataChannel.close()
        _streamingDataChannel.close()

        characteristicsByUuid.update { emptyMap() }
        serialWrite.value = null
        characteristicValueState.emit(emptyMap())
        _metaInfoKeysStream.emit(emptyMap())
        _stateStream.emit(FPeripheralState.DISCONNECTED)
    }

    override fun onError(error: NSError) {
        debug { "#onError ${error.localizedDescription}" }
        val domain = error.domain
        val code = error.code

        when (domain) {
            "CBATTErrorDomain" -> handleCBATTError(code)
            "CBErrorDomain" -> handleCBError(code)
        }
    }

    @Suppress("MagicNumber")
    private fun handleCBError(code: Long) {
        debug { "Peripheral CBError id=${identifier.UUIDString} code=$code" }

        scope.launch {
            when (code) {
                CBErrorPeerRemovedPairingInformation ->
                    _stateStream.emit(FPeripheralState.INVALID_PAIRING)
                CBErrorEncryptionTimedOut -> {
                    _stateStream.emit(FPeripheralState.DISCONNECTED)
                }
            }
        }
    }

    @Suppress("MagicNumber")
    private fun handleCBATTError(code: Long) {
        debug { "Peripheral CBATTError id=${identifier.UUIDString} code=$code" }
        scope.launch {
            when (code) {
                CBATTErrorInsufficientEncryption ->
                    _stateStream.emit(FPeripheralState.PAIRING_FAILED)
            }
        }
    }

    override suspend fun writeValue(data: ByteArray) {
        debug { "#writeValue with ${data.size}" }
        gattIO.writeSerial(data)
    }

    override suspend fun readValue(characteristicUuid: Uuid): ByteArray {
        debug { "#readValue from $characteristicUuid" }
        return gattIO.readValue(characteristicUuid)
    }

    override suspend fun writeValue(characteristicUuid: Uuid, data: ByteArray) {
        debug { "#writeValue for $characteristicUuid with ${data.size}" }
        gattIO.writeValue(characteristicUuid, data)
    }

    internal fun handleDidDiscoverServices(
        peripheral: CBPeripheral,
        didDiscoverServices: NSError?
    ) {
        debug { "#handleDidDiscoverServices ${peripheral.identifier} error $didDiscoverServices" }
        discovery.handleDidDiscoverServices(
            peripheral = peripheral,
            didDiscoverServices = didDiscoverServices
        )
    }

    internal fun didDiscoverCharacteristics(
        service: CBService,
        error: NSError?
    ) {
        debug { "#didDiscoverCharacteristics ${service.UUID} error $error" }
        discovery.didDiscoverCharacteristics(
            peripheral = peripheral,
            service = service,
            error = error
        )
    }

    internal fun didUpdateValue(
        characteristic: CBCharacteristic,
        error: NSError?
    ) {
        debug { "#didUpdateValue ${characteristic.UUID} error $error" }
        valueRouter.didUpdateValue(
            characteristic = characteristic,
            error = error
        )
    }

    internal fun handleDidWriteValue(
        didWriteValueForCharacteristic: CBCharacteristic,
        error: NSError?
    ) {
        debug { "#handleDidWriteValue ${didWriteValueForCharacteristic.UUID} error $error" }
        gattIO.handleDidWriteValue(
            didWriteValueForCharacteristic = didWriteValueForCharacteristic,
            error = error
        )
    }
}
