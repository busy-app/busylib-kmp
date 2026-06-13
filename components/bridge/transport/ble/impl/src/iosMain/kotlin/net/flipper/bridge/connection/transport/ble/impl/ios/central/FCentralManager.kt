package net.flipper.bridge.connection.transport.ble.impl.ios.central

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.ble.impl.BleConstants
import net.flipper.bridge.connection.transport.ble.impl.ios.peripheral.FPeripheral
import net.flipper.bridge.connection.transport.ble.impl.ios.peripheral.FPeripheralApi
import net.flipper.bridge.connection.transport.ble.impl.ios.peripheral.FPeripheralState
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.warn
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBManagerState
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.NSUUID
import platform.darwin.dispatch_queue_attr_make_with_qos_class
import platform.darwin.dispatch_queue_create
import platform.posix.QOS_CLASS_USER_INITIATED

@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, binding = binding<FCentralManagerApi>())
class FCentralManager internal constructor(
    scope: CoroutineScope,
    centralManagerProvider: (FCentralManagerDelegate) -> CBCentralManager
) : FCentralManagerApi, LogTagProvider {
    private val manager: CBCentralManager

    @Inject
    constructor(scope: CoroutineScope) : this(
        scope = scope,
        centralManagerProvider = ::createCentralManager
    )

    override val TAG: String
        get() = "FCentralManager"

    private val _connectedStream = MutableStateFlow<Map<NSUUID, FPeripheralApi>>(emptyMap())
    override val connectedStream: WrappedStateFlow<Map<NSUUID, FPeripheralApi>> =
        _connectedStream.asStateFlow().wrap()

    private val _discoveredStream = MutableStateFlow<Set<DiscoveredBluetoothDevice>>(emptySet())

    override val discoveredStream: WrappedStateFlow<Set<DiscoveredBluetoothDevice>> =
        _discoveredStream.asStateFlow().wrap()

    private val _bleStatusStream = MutableStateFlow(FBLEStatus.UNKNOWN)
    override val bleStatusStream: WrappedStateFlow<FBLEStatus> =
        _bleStatusStream.asStateFlow().wrap()

    private val delegate = FCentralManagerDelegate(
        onError = { event, throwable ->
            error(throwable) { "Failed to send event $event" }
        }
    )

    init {
        manager = centralManagerProvider(delegate)

        scope.launch {
            for (event in delegate.events) {
                processEvent(event)
            }
        }
    }

    private suspend fun processEvent(event: FCentralManagerEvent) {
        when (event) {
            is FCentralManagerEvent.StateUpdated -> updateBLEStatus(event.state)
            is FCentralManagerEvent.DidConnect -> didConnect(event.peripheral)
            is FCentralManagerEvent.DidDisconnect -> didDisconnect(event.peripheral, event.error)
            is FCentralManagerEvent.DidFailToConnect -> didFailToConnect(
                event.peripheral,
                event.error
            )

            is FCentralManagerEvent.DidDiscover -> didDiscover(
                event.peripheral,
                event.advertisementData,
                event.rssi
            )
        }
    }

    override suspend fun connect(
        scope: CoroutineScope,
        config: FBleDeviceConnectionConfig
    ) {
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

        _connectedStream.update {
            it + (peripheral.identifier to device)
        }

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

        withTimeoutOrNull(BleConstants.DISCONNECT_TIME) {
            peripheral.stateStream.first {
                it == FPeripheralState.DISCONNECTED ||
                    it == FPeripheralState.PAIRING_CANCELLED ||
                    it == FPeripheralState.DEVICE_FORGOT_PAIRING
            }
        } ?: run {
            warn { "Disconnect timeout for peripheral id=$id, forcing cleanup" }
            peripheral.onDisconnect()
        }
    }

    override suspend fun startScan() {
        info {
            "#startScan delegate=${manager.delegate} rawState=${manager.state} "
        }
        withTimeoutOrNull(BleConstants.CONNECT_TIME) {
            bleStatusStream.first { it == FBLEStatus.POWERED_ON }
        } ?: run {
            warn { "Cannot start scan with BLE state ${bleStatusStream.first()}" }
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

        info { "BLE state updated: raw=$state mapped=$newStatus" }

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

        _connectedStream.update {
            it + (peripheral.identifier to device)
        }
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

        val toDisconnect = _connectedStream.first()[peripheral.identifier] ?: run {
            error { "CB didDisconnect for unknown peripheral id=${peripheral.identifier}" }
            return
        }
        toDisconnect.onDisconnect()

        _connectedStream.update { connected ->
            connected - peripheral.identifier
        }
        info { "CB didDisconnect id=${peripheral.identifier}" }
    }

    private suspend fun didFailToConnect(
        peripheral: CBPeripheral,
        error: NSError?
    ) {
        info { "#didFailToConnect peripheral=${peripheral.identifier} error=$error" }
        _connectedStream.update { connected ->
            val device = connected[peripheral.identifier] ?: run {
                error { "CB didFailToConnect for unknown peripheral id=${peripheral.identifier}" }
                return@update connected
            }

            if (error == null) {
                error { "CB didFailToConnect without error id=${peripheral.identifier}" }
            } else {
                device.onError(error)
            }
            connected - peripheral.identifier
        }
        error { "CB didFailToConnect id=${peripheral.identifier} error=$error" }
    }

    private suspend fun didDiscover(
        peripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        rssi: NSNumber
    ) {
        info { "#didDiscover peripheral=${peripheral.identifier} rssi=$rssi ad=$advertisementData" }
        val uuid = peripheral.identifier
        val name = peripheral.name

        val devices = _discoveredStream.updateAndGet { current ->
            val device = DiscoveredBluetoothDevice(id = uuid, name = name)
            current + device
        }
        info { "Emitted to discovered stream, total devices: ${devices.size}" }
    }
}

private fun CBCentralManager.retrievePeripheral(id: NSUUID): CBPeripheral? {
    val list: List<NSUUID> = listOf(id)
    val peripherals = this.retrievePeripheralsWithIdentifiers(list)
    return peripherals.firstOrNull() as? CBPeripheral
}

@OptIn(ExperimentalForeignApi::class)
private fun createCentralManager(delegate: FCentralManagerDelegate): CBCentralManager {
    val attr = dispatch_queue_attr_make_with_qos_class(
        null, // DISPATCH_QUEUE_SERIAL = NULL in Darwin headers
        QOS_CLASS_USER_INITIATED,
        0
    )
    val bluetoothQueue = dispatch_queue_create(
        "net.flipper.bridge.connection.transport.ble.impl.ios.central",
        attr
    )

    return CBCentralManager(
        delegate = delegate,
        queue = bluetoothQueue
    )
}
