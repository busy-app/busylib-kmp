package com.flipperdevices.transport.ble.impl.cb

import com.flipperdevices.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import com.flipperdevices.core.log.LogTagProvider
import com.flipperdevices.core.log.error
import com.flipperdevices.core.log.info
import com.flipperdevices.core.log.warn
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBManagerState
import platform.CoreBluetooth.CBPeripheral
import platform.Foundation.NSError
import platform.Foundation.NSUUID
import platform.darwin.NSObject

interface FCentralManagerApi {
    val connectedStream: StateFlow<Map<NSUUID, FPeripheralApi>>
    val bleStatusStream: StateFlow<FBLEStatus>

    suspend fun connect(config: FBleDeviceConnectionConfig)
    suspend fun disconnect(id: NSUUID)
    suspend fun startScan()
    suspend fun stopScan()
}

class FCentralManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
) : FCentralManagerApi,
    NSObject(),
    CBCentralManagerDelegateProtocol,
    LogTagProvider {

    override val TAG: String
        get() = "FCentralManager"

    private val _connectedStream = MutableStateFlow<Map<NSUUID, FPeripheralApi>>(emptyMap())
    override val connectedStream: StateFlow<Map<NSUUID, FPeripheralApi>> = _connectedStream.asStateFlow()

    private val _discoveredStream = MutableStateFlow<Map<NSUUID, FPeripheralApi>>(emptyMap())
    private val discoveredStream: StateFlow<Map<NSUUID, FPeripheralApi>> = _discoveredStream.asStateFlow()

    private val _bleStatusStream = MutableStateFlow<FBLEStatus>(FBLEStatus.UNKNOWN)
    override val bleStatusStream: StateFlow<FBLEStatus> = _bleStatusStream.asStateFlow()

    private val manager: CBCentralManager = CBCentralManager()

    init {
        manager.delegate = this
    }

    override suspend fun connect(config: FBleDeviceConnectionConfig) {
        val id = config.macAddress
        val uuid = NSUUID(id)

        val peripheral: CBPeripheral = manager.retrievePeripheral(uuid) ?: run {
            error { "Requested connect for unknown peripheral id=$id" }
            return
        }

        val device = FPeripheral(peripheral, config, scope)
        device.onConnecting()

        val current = _connectedStream.value
        _connectedStream.emit(current + (peripheral.identifier to device))

        info { "CB connect requested id=$id" }
        manager.connectPeripheral(peripheral, options = null)
    }

    override suspend fun disconnect(id: NSUUID) {
        val cbPeripheral = manager.retrievePeripheral(id) ?: run {
            warn { "Requested disconnect for unknown peripheral id=$id" }
            return
        }
        val peripheral = _connectedStream.value[id] ?: run {
            warn { "Requested disconnect for not connected peripheral id=$id" }
            return
        }

        info { "CB disconnect requested id=$id" }
        peripheral.onDisconnecting()
        manager.cancelPeripheralConnection(cbPeripheral)
    }

    // Find by busy bar UUID
    override suspend fun startScan() {
        val state = FBLEStatus.from(manager.state)
        if (state != FBLEStatus.POWERED_ON) {
            warn { "Cannot start scan with BLE state: $state" }
            return
        }

        manager.scanForPeripheralsWithServices(
            serviceUUIDs = null,
            options = null
        )
        info { "Scan started" }
    }

    override suspend fun stopScan() {
        if (manager.isScanning()) {
            manager.stopScan()
            _discoveredStream.emit(emptyMap())
            info { "Scan stopped" }
        }
    }

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        val state = central.state
        scope.launch { updateBLEStatus(state) }
    }

    private suspend fun updateBLEStatus(state: CBManagerState) {
        val newStatus = FBLEStatus.from(state)
        _bleStatusStream.emit(newStatus)

        info { "BLE state updated: $state" }

        if (newStatus != FBLEStatus.POWERED_ON) {
            _discoveredStream.emit(emptyMap())

            _connectedStream.value.values.forEach { peripheral ->
                peripheral.onDisconnect()
            }

            _connectedStream.emit(emptyMap())
            info { "Disconnected all due to state change" }
        }
    }

    override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
        scope.launch {
            didConnect(didConnectPeripheral)
        }
    }

    private suspend fun didConnect(peripheral: CBPeripheral) {
        info { "didConnect" }
        val device = _connectedStream.value[peripheral.identifier] ?: run {
            this.error { "CB didConnect for unknown peripheral id=${peripheral.identifier}" }
            return
        }

        val current = _connectedStream.value
        _connectedStream.emit(current + (peripheral.identifier to device))
        device.onConnect()
        info { "CB didConnect id=${peripheral.identifier}" }
    }

    @ObjCSignatureOverride
    override fun centralManager(
        central: CBCentralManager,
        didDisconnectPeripheral: CBPeripheral,
        error: NSError?
    ) {
        scope.launch {
            didDisconnect(didDisconnectPeripheral, error)
        }
    }

    private suspend fun didDisconnect(
        peripheral: CBPeripheral,
        error: NSError?
    ) {
        info { "didDisconnect" }
        val connected = _connectedStream.value
        val device = connected[peripheral.identifier] ?: run {
            this.error { "CB didDisconnect for unknown peripheral id=${peripheral.identifier}" }
            return
        }

        device.onDisconnect()
        _connectedStream.emit(connected - peripheral.identifier)

        if (error != null) {
            warn { "CB didDisconnect id=${peripheral.identifier} error=$error" }
        } else {
            info { "CB didDisconnect id=${peripheral.identifier}" }
        }
    }

    @ObjCSignatureOverride
    override fun centralManager(
        central: CBCentralManager,
        didFailToConnectPeripheral: CBPeripheral,
        error: NSError?
    ) {
        scope.launch {
            didFailToConnect(didFailToConnectPeripheral, error)
        }
    }

    private suspend fun didFailToConnect(peripheral: CBPeripheral, error: NSError?) {
        info { "didFailToConnect" }
        val connected = _connectedStream.value
        val device = connected[peripheral.identifier] ?: run {
            this.error { "CB didFailToConnect for unknown peripheral id=${peripheral.identifier}" }
            return
        }

        if (error == null) {
            this.error { "CB didFailToConnect without error id=${peripheral.identifier}" }
            return
        }

        device.onError(error)
        _connectedStream.emit(connected - peripheral.identifier)
        this.error { "CB didFailToConnect id=${peripheral.identifier} error=$error" }
    }
}

private fun CBCentralManager.retrievePeripheral(id: NSUUID): CBPeripheral? {
    val list: List<*> = listOf(id)
    return this.retrievePeripheralsWithIdentifiers(list).firstOrNull() as? CBPeripheral
}
