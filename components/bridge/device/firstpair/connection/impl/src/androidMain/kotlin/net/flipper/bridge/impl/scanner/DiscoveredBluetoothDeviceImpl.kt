package net.flipper.bridge.impl.scanner

import net.flipper.bridge.api.scanner.DiscoveredBluetoothDevice
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.ble.client.android.ScanResult
import kotlin.uuid.Uuid

class DiscoveredBluetoothDeviceImpl(
    override val device: Peripheral,
    private var lastScanResult: ScanResult? = null,
    private var nameInternal: String? = device.name,
    private var servicesResult: List<Uuid>? = null
) : DiscoveredBluetoothDevice.RealDiscoveredBluetoothDevice {
    // Wrapper for data variables
    override val address: String get() = device.address
    override val name: String? get() = nameInternal
    override val services: List<Uuid> get() = servicesResult.orEmpty()

    val scanResult: ScanResult? get() = lastScanResult

    constructor(scanResult: ScanResult) : this(
        device = scanResult.peripheral,
        lastScanResult = scanResult,
        nameInternal = scanResult.peripheral.name,
        servicesResult = scanResult.advertisingData.serviceUuids
    )

    fun update(scanResult: ScanResult) {
        lastScanResult = scanResult
        nameInternal = scanResult.peripheral.name
        servicesResult = scanResult.advertisingData.serviceUuids
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Peripheral -> {
                device.address == other.address
            }

            is DiscoveredBluetoothDevice -> {
                device.address == other.address
            }

            else -> false
        }
    }

    override fun toString(): String {
        return "DiscoveredBluetoothDeviceImpl(" +
            "device=$device," +
            "lastScanResult=$lastScanResult," +
            "nameInternal=$nameInternal," +
            "servicesResult=$servicesResult," +
            "servicesResult=$servicesResult," +
            "name=$name)"
    }

    override fun hashCode() = device.hashCode()
}
