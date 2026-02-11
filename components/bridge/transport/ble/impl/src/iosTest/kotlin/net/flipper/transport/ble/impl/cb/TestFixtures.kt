package net.flipper.transport.ble.impl.cb

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceSerialConfig
import net.flipper.bridge.connection.transport.ble.api.GATTCharacteristicAddress
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.transport.ble.impl.toByteArray
import net.flipper.transport.ble.impl.toNSData
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBMutableCharacteristic
import platform.CoreBluetooth.CBMutableService
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBService
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.NSUUID
import platform.Foundation.setValue
import kotlin.uuid.Uuid

internal const val SERIAL_SERVICE_SHORT_UUID = "308A"
internal const val SERIAL_RX_SHORT_UUID = "308B"
internal const val SERIAL_TX_SHORT_UUID = "308C"
internal const val META_SERVICE_SHORT_UUID = "180A"
internal const val DEVICE_NAME_SHORT_UUID = "2A00"
internal const val BATTERY_LEVEL_SHORT_UUID = "2A19"
internal const val MANUFACTURER_SHORT_UUID = "2A29"

private const val SERIAL_SERVICE_FULL_UUID = "0000308a-0000-1000-8000-00805f9b34fb"
private const val SERIAL_RX_FULL_UUID = "0000308b-0000-1000-8000-00805f9b34fb"
private const val SERIAL_TX_FULL_UUID = "0000308c-0000-1000-8000-00805f9b34fb"
private const val META_SERVICE_FULL_UUID = "0000180a-0000-1000-8000-00805f9b34fb"
private const val DEVICE_NAME_FULL_UUID = "00002a00-0000-1000-8000-00805f9b34fb"
private const val BATTERY_LEVEL_FULL_UUID = "00002a19-0000-1000-8000-00805f9b34fb"
private const val MANUFACTURER_FULL_UUID = "00002a29-0000-1000-8000-00805f9b34fb"

internal data class WriteRequest(
    val value: ByteArray,
    val characteristicUuid: String,
    val type: Long,
)

@OptIn(ExperimentalForeignApi::class)
internal class RecordingPeripheral : CBPeripheral() {
    init {
        setValue(NSUUID(), forKey = "identifier")
    }

    var discoverServicesCalls: Int = 0
        private set

    val discoverCharacteristicsRequests: MutableList<CBService> = mutableListOf()
    val notifyRequests: MutableList<Pair<Boolean, String>> = mutableListOf()
    val readRequests: MutableList<String> = mutableListOf()
    val writeRequests: MutableList<WriteRequest> = mutableListOf()

    override fun discoverServices(serviceUUIDs: List<*>?) {
        discoverServicesCalls += 1
    }

    override fun discoverCharacteristics(characteristicUUIDs: List<*>?, forService: CBService) {
        discoverCharacteristicsRequests += forService
    }

    override fun setNotifyValue(enabled: Boolean, forCharacteristic: CBCharacteristic) {
        notifyRequests += enabled to forCharacteristic.UUID.UUIDString
    }

    override fun readValueForCharacteristic(characteristic: CBCharacteristic) {
        readRequests += characteristic.UUID.UUIDString
    }

    override fun writeValue(data: platform.Foundation.NSData, forCharacteristic: CBCharacteristic, type: Long) {
        writeRequests += WriteRequest(
            value = data.toByteArray(),
            characteristicUuid = forCharacteristic.UUID.UUIDString,
            type = type,
        )
    }

    fun setStateRaw(raw: Long) {
        setValue(raw, forKey = "state")
    }

    fun setServicesRaw(services: List<CBService>) {
        setValue(services, forKey = "services")
    }
}

@OptIn(ExperimentalForeignApi::class)
internal class RecordingCentralManager : CBCentralManager(delegate = null, queue = null, options = null) {
    private val peripherals: MutableMap<NSUUID, CBPeripheral> = mutableMapOf()

    val connectRequests: MutableList<CBPeripheral> = mutableListOf()
    val cancelRequests: MutableList<CBPeripheral> = mutableListOf()
    val scanRequests: MutableList<List<*>> = mutableListOf()

    var stopScanCalls: Int = 0
        private set

    var scanning: Boolean = false

    override fun retrievePeripheralsWithIdentifiers(identifiers: List<*>): List<*> {
        return identifiers
            .mapNotNull { it as? NSUUID }
            .mapNotNull { peripherals[it] }
    }

    override fun connectPeripheral(peripheral: CBPeripheral, options: Map<Any?, *>?) {
        connectRequests += peripheral
    }

    override fun cancelPeripheralConnection(peripheral: CBPeripheral) {
        cancelRequests += peripheral
    }

    override fun scanForPeripheralsWithServices(serviceUUIDs: List<*>?, options: Map<Any?, *>?) {
        scanning = true
        scanRequests += serviceUUIDs ?: emptyList<Any?>()
    }

    override fun stopScan() {
        scanning = false
        stopScanCalls += 1
    }

    override fun isScanning(): Boolean = scanning

    fun registerPeripheral(peripheral: CBPeripheral) {
        peripherals[peripheral.identifier] = peripheral
    }

    fun setStateRaw(raw: Long) {
        setValue(raw, forKey = "state")
    }

    fun emitStateUpdate() {
        val centralDelegate = checkNotNull(delegate)
        centralDelegate.centralManagerDidUpdateState(this)
    }

    fun emitDidConnect(peripheral: CBPeripheral) {
        val centralDelegate = checkNotNull(delegate)
        centralDelegate.centralManager(this, didConnectPeripheral = peripheral)
    }

    fun emitDidDisconnect(peripheral: CBPeripheral, error: NSError?) {
        val centralDelegate = checkNotNull(delegate)
        centralDelegate.centralManager(this, didDisconnectPeripheral = peripheral, error = error)
    }

    fun emitDidFailToConnect(peripheral: CBPeripheral, error: NSError?) {
        val centralDelegate = checkNotNull(delegate)
        centralDelegate.centralManager(this, didFailToConnectPeripheral = peripheral, error = error)
    }

    fun emitDidDiscover(peripheral: CBPeripheral, rssi: Int = -42) {
        val centralDelegate = checkNotNull(delegate)
        centralDelegate.centralManager(
            this,
            didDiscoverPeripheral = peripheral,
            advertisementData = emptyMap<Any?, Any?>(),
            RSSI = NSNumber(integer = rssi.toLong()),
        )
    }
}

internal fun createConfig(
    macAddress: String,
    metaInfoGattMap: ImmutableMap<TransportMetaInfoKey, GATTCharacteristicAddress> = persistentMapOf(
        TransportMetaInfoKey.DEVICE_NAME to GATTCharacteristicAddress(
            serviceAddress = Uuid.parse(META_SERVICE_FULL_UUID),
            characteristicAddress = Uuid.parse(DEVICE_NAME_FULL_UUID),
        )
    )
): FBleDeviceConnectionConfig {
    return FBleDeviceConnectionConfig(
        deviceName = "Busy Device",
        macAddress = macAddress,
        serialConfig = FBleDeviceSerialConfig(
            serialServiceUuid = Uuid.parse(SERIAL_SERVICE_FULL_UUID),
            rxServiceCharUuid = Uuid.parse(SERIAL_RX_FULL_UUID),
            txServiceCharUuid = Uuid.parse(SERIAL_TX_FULL_UUID),
        ),
        metaInfoGattMap = metaInfoGattMap,
    )
}

internal fun newCharacteristic(
    shortUuid: String,
    payload: ByteArray? = null,
): CBMutableCharacteristic {
    return CBMutableCharacteristic(
        type = CBUUID.UUIDWithString(shortUuid),
        properties = 0u,
        value = payload?.toNSData(),
        permissions = 0u,
    )
}

@OptIn(ExperimentalForeignApi::class)
internal fun newService(
    shortUuid: String,
    characteristics: List<CBCharacteristic>,
): CBMutableService {
    return CBMutableService(
        type = CBUUID.UUIDWithString(shortUuid),
        primary = true,
    ).also {
        it.setValue(characteristics, forKey = "characteristics")
    }
}

internal fun error(domain: String, code: Long): NSError {
    return NSError.errorWithDomain(
        domain = domain,
        code = code,
        userInfo = null,
    )
}

internal fun batteryAndManufacturerMetaMap(): ImmutableMap<TransportMetaInfoKey, GATTCharacteristicAddress> {
    return persistentMapOf(
        TransportMetaInfoKey.BATTERY_LEVEL to GATTCharacteristicAddress(
            serviceAddress = Uuid.parse(META_SERVICE_FULL_UUID),
            characteristicAddress = Uuid.parse(BATTERY_LEVEL_FULL_UUID),
        ),
        TransportMetaInfoKey.MANUFACTURER to GATTCharacteristicAddress(
            serviceAddress = Uuid.parse(META_SERVICE_FULL_UUID),
            characteristicAddress = Uuid.parse(MANUFACTURER_FULL_UUID),
        )
    )
}
