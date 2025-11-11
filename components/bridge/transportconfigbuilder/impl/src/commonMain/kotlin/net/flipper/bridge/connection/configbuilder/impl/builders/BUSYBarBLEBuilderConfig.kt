package net.flipper.bridge.connection.configbuilder.impl.builders

import kotlinx.collections.immutable.persistentMapOf
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceSerialConfig
import net.flipper.bridge.connection.transport.ble.api.GATTCharacteristicAddress
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import kotlin.uuid.Uuid

private val INFORMATION_SERVICE_UUID = Uuid.parse("0000180a-0000-1000-8000-00805f9b34fb")
private val BATTERY_SERVICE_UUID = Uuid.parse("0000180f-0000-1000-8000-00805f9b34fb")

@Inject
class BUSYBarBLEBuilderConfig {
    fun build(
        address: String
    ) = FBleDeviceConnectionConfig(
        macAddress = address,
        serialConfig = FBleDeviceSerialConfig(
            serialServiceUuid = Uuid.parse("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"),
            txServiceCharUuid = Uuid.parse("6E400002-B5A3-F393-E0A9-E50E24DCCA9E"), // Mobile -> Device
            rxServiceCharUuid = Uuid.parse("6E400003-B5A3-F393-E0A9-E50E24DCCA9E") // Device -> Mobile
        ),
        metaInfoGattMap = persistentMapOf(
            TransportMetaInfoKey.DEVICE_NAME to GATTCharacteristicAddress(
                serviceAddress = Uuid.parse("00001800-0000-1000-8000-00805f9b34fb"),
                characteristicAddress = Uuid.parse("00002a00-0000-1000-8000-00805f9b34fb")
            ),
            TransportMetaInfoKey.MANUFACTURER to GATTCharacteristicAddress(
                serviceAddress = INFORMATION_SERVICE_UUID,
                characteristicAddress = Uuid.parse("00002a29-0000-1000-8000-00805f9b34fb")
            ),
            TransportMetaInfoKey.HARDWARE_VERSION to GATTCharacteristicAddress(
                serviceAddress = INFORMATION_SERVICE_UUID,
                characteristicAddress = Uuid.parse("00002a27-0000-1000-8000-00805f9b34fb")
            ),
            TransportMetaInfoKey.SOFTWARE_VERSION to GATTCharacteristicAddress(
                serviceAddress = INFORMATION_SERVICE_UUID,
                characteristicAddress = Uuid.parse("00002a26-0000-1000-8000-00805f9b34fb")
            ),
            TransportMetaInfoKey.BATTERY_LEVEL to GATTCharacteristicAddress(
                serviceAddress = BATTERY_SERVICE_UUID,
                characteristicAddress = Uuid.parse("00002a19-0000-1000-8000-00805f9b34fb")
            ),
            TransportMetaInfoKey.BATTERY_POWER_STATE to GATTCharacteristicAddress(
                serviceAddress = BATTERY_SERVICE_UUID,
                characteristicAddress = Uuid.parse("00002BED-0000-1000-8000-00805F9B34FB")
            )
        )
    )
}
