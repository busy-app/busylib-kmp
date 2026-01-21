package net.flipper.transport.ble.impl.cb

import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.warn
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBManagerState
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.NSUUID
import platform.darwin.NSObject

interface FCentralManagerApi {
    val connectedStream: WrappedStateFlow<Map<NSUUID, FPeripheralApi>>
    val bleStatusStream: WrappedStateFlow<FBLEStatus>
    val discoveredStream: WrappedStateFlow<Set<NSUUID>>

    suspend fun connect(config: FBleDeviceConnectionConfig)
    suspend fun disconnect(id: NSUUID)
    suspend fun startScan()
    suspend fun stopScan()
}

private class FCentralManagerDelegate(
    private val onStateUpdate: (CBManagerState) -> Unit,
    private val onDidConnect: (CBPeripheral) -> Unit,
    private val onDidDisconnect: (CBPeripheral, NSError?) -> Unit,
    private val onDidFailToConnect: (CBPeripheral, NSError?) -> Unit,
    private val onDidDiscover: (CBPeripheral, Map<Any?, *>, NSNumber) -> Unit
) : NSObject(), CBCentralManagerDelegateProtocol {

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        onStateUpdate(central.state)
    }

    override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
        onDidConnect(didConnectPeripheral)
    }

    @ObjCSignatureOverride
    override fun centralManager(
        central: CBCentralManager,
        didDisconnectPeripheral: CBPeripheral,
        error: NSError?
    ) {
        onDidDisconnect(didDisconnectPeripheral, error)
    }

    @ObjCSignatureOverride
    override fun centralManager(
        central: CBCentralManager,
        didFailToConnectPeripheral: CBPeripheral,
        error: NSError?
    ) {
        onDidFailToConnect(didFailToConnectPeripheral, error)
    }

    override fun centralManager(
        central: CBCentralManager,
        didDiscoverPeripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        RSSI: NSNumber
    ) {
        onDidDiscover(didDiscoverPeripheral, advertisementData, RSSI)
    }
}

class FCentralManager(
    private val scope: CoroutineScope = CoroutineScope(FlipperDispatchers.default),
    val manager: CBCentralManager
) : FCentralManagerApi, LogTagProvider {

    override val TAG: String
        get() = "FCentralManager"

    private val _connectedStream = MutableStateFlow<Map<NSUUID, FPeripheralApi>>(emptyMap())
    override val connectedStream: WrappedStateFlow<Map<NSUUID, FPeripheralApi>> = _connectedStream.asStateFlow().wrap()

    private val _discoveredStream = MutableStateFlow<Set<NSUUID>>(emptySet())

    @Suppress("UnusedPrivateProperty")
    override val discoveredStream: WrappedStateFlow<Set<NSUUID>> = _discoveredStream.asStateFlow().wrap()

    private val _bleStatusStream = MutableStateFlow<FBLEStatus>(FBLEStatus.UNKNOWN)
    override val bleStatusStream: WrappedStateFlow<FBLEStatus> = _bleStatusStream.asStateFlow().wrap()

    private val delegate = FCentralManagerDelegate(
        onStateUpdate = { state -> scope.launch { updateBLEStatus(state) } },
        onDidConnect = { peripheral -> scope.launch { didConnect(peripheral) } },
        onDidDisconnect = { peripheral, error -> scope.launch { didDisconnect(peripheral, error) } },
        onDidFailToConnect = { peripheral, error -> scope.launch { didFailToConnect(peripheral, error) } },
        onDidDiscover = { peripheral, adv, rssi -> scope.launch { didDiscover(peripheral, adv, rssi) } }
    )

    init {
        manager.delegate = delegate
    }

    override suspend fun connect(config: FBleDeviceConnectionConfig) {
        info { "#connect config=$config" }
        val id = config.macAddress
        val uuid = NSUUID(id)

        val peripheral: CBPeripheral = manager.retrievePeripheral(uuid) ?: run {
            error { "Requested connect for unknown peripheral id=$id" }
            return
        }

        info { "CB connect preparing id=$id" }
        val device = FPeripheral(peripheral, config, scope)
        device.onConnecting()

        val current = _connectedStream.first()
        _connectedStream.emit(current + (peripheral.identifier to device))

        info { "CB connect requested id=$id" }
        manager.connectPeripheral(peripheral, options = null)
    }

    override suspend fun disconnect(id: NSUUID) {
        info { "#disconnect id=$id" }
        val cbPeripheral = manager.retrievePeripheral(id) ?: run {
            warn { "Requested disconnect for unknown peripheral id=$id" }
            return
        }
        val peripheral = _connectedStream.first()[id] ?: run {
            warn { "Requested disconnect for not connected peripheral id=$id" }
            return
        }

        info { "CB disconnect requested id=$id" }
        peripheral.onDisconnecting()
        manager.cancelPeripheralConnection(cbPeripheral)

        peripheral.stateStream.first {
            it == FPeripheralState.DISCONNECTED ||
                it == FPeripheralState.PAIRING_FAILED ||
                it == FPeripheralState.INVALID_PAIRING
        }
    }

    override suspend fun startScan() {
        info { "#startScan" }
        val state = FBLEStatus.from(manager.state)
        if (state != FBLEStatus.POWERED_ON) {
            warn { "Cannot start scan with BLE state: $state" }
            return
        }

        manager.scanForPeripheralsWithServices(
            serviceUUIDs = listOf(CBUUID.UUIDWithString("308A")),
            options = null
        )
        info { "Scan started ${manager.delegate}" }
    }

    override suspend fun stopScan() {
        info { "#stopScan" }
        if (manager.isScanning()) {
            manager.stopScan()
            _discoveredStream.emit(emptySet())
            info { "Scan stopped" }
        }
    }

    private suspend fun updateBLEStatus(state: CBManagerState) {
        info { "#updateBLEStatus state=$state" }
        val newStatus = FBLEStatus.from(state)
        _bleStatusStream.emit(newStatus)

        info { "BLE state updated: $state" }

        if (newStatus != FBLEStatus.POWERED_ON) {
            _discoveredStream.emit(emptySet())

            _connectedStream.first().values.forEach { peripheral ->
                peripheral.onDisconnect()
            }

            _connectedStream.emit(emptyMap())
            info { "Disconnected all due to state change" }
        }
    }

    private suspend fun didConnect(peripheral: CBPeripheral) {
        info { "#didConnect peripheral=${peripheral.identifier}" }
        val device = _connectedStream.first()[peripheral.identifier] ?: run {
            error { "CB didConnect for unknown peripheral id=${peripheral.identifier}" }
            return
        }

        val current = _connectedStream.first()
        _connectedStream.emit(current + (peripheral.identifier to device))
        device.onConnect()
        info { "CB didConnect id=${peripheral.identifier}" }
    }

    private suspend fun didDisconnect(
        peripheral: CBPeripheral,
        error: NSError?
    ) {
        info { "#didDisconnect peripheral=${peripheral.identifier} error=$error" }
        if (error != null) {
            error { "didDisconnect with error: $error" }
        }

        val connected = _connectedStream.first()
        val device = connected[peripheral.identifier] ?: run {
            error { "CB didDisconnect for unknown peripheral id=${peripheral.identifier}" }
            return
        }

        device.onDisconnect()
        _connectedStream.emit(connected - peripheral.identifier)
        info { "CB didDisconnect id=${peripheral.identifier}" }
    }

    private suspend fun didFailToConnect(
        peripheral: CBPeripheral,
        error: NSError?
    ) {
        info { "#didFailToConnect peripheral=${peripheral.identifier} error=$error" }
        val connected = _connectedStream.first()
        val device = connected[peripheral.identifier] ?: run {
            error { "CB didFailToConnect for unknown peripheral id=${peripheral.identifier}" }
            return
        }

        if (error == null) {
            error { "CB didFailToConnect without error id=${peripheral.identifier}" }
            return
        }
        device.onError(error)
        _connectedStream.emit(connected - peripheral.identifier)
        error { "CB didFailToConnect id=${peripheral.identifier} error=$error" }
    }

    @Suppress("UnusedParameter")
    private suspend fun didDiscover(
        peripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        rssi: NSNumber
    ) {
        info { "#didDiscover peripheral=${peripheral.identifier} rssi=$rssi" }
        val uuid = peripheral.identifier

        val current = _discoveredStream.value
        _discoveredStream.emit(current + uuid)
        info { "Emitted to discovered stream, total devices: ${current.size + 1}" }
    }
}

private fun CBCentralManager.retrievePeripheral(id: NSUUID): CBPeripheral? {
    val list: List<NSUUID> = listOf(id)
    val peripherals = this.retrievePeripheralsWithIdentifiers(list)
    return peripherals.firstOrNull() as? CBPeripheral
}
