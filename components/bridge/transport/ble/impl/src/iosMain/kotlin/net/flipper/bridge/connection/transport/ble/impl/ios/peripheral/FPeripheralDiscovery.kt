package net.flipper.bridge.connection.transport.ble.impl.ios.peripheral

import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.warn
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBService
import platform.Foundation.NSError
import kotlin.uuid.Uuid

internal class FPeripheralDiscovery(
    private val config: FBleDeviceConnectionConfig,
    private val characteristicsByUuid: MutableMap<Uuid, CBCharacteristic>,
    private val serialWriteUpdater: (CBCharacteristic?) -> Unit,
    private val identifierProvider: () -> String,
) : LogTagProvider {

    override val TAG: String = "FPeripheralDiscovery"

    fun handleDidDiscoverServices(
        peripheral: CBPeripheral,
        didDiscoverServices: NSError?
    ) {
        if (didDiscoverServices != null) {
            error { "Service discovery failed id=${identifierProvider()} error=$didDiscoverServices" }
            return
        }

        peripheral.services?.forEach { service ->
            val cbService = service as CBService
            peripheral.discoverCharacteristics(null, forService = cbService)
        }
    }

    fun didDiscoverCharacteristics(
        peripheral: CBPeripheral,
        service: CBService,
        error: NSError?
    ) {
        if (error != null) {
            error { "Characteristic discovery failed id=${identifierProvider()} error=$error" }
            return
        }

        val characteristics = service.characteristics?.map { it as CBCharacteristic } ?: emptyList()
        characteristics.forEach { characteristic ->
            val characteristicUuid = characteristic.UUID.toKotlinUUID()
            characteristicsByUuid[characteristicUuid] = characteristic
            debug { "Registered characteristic UUID: $characteristicUuid" }
        }

        val serviceUUID = service.UUID.toKotlinUUID()
        info { "Service $service UUID ${service.UUID} Kotlin $serviceUUID" }

        if (serviceUUID == config.serialConfig.serialServiceUuid) {
            val serialRead = characteristicsByUuid[config.serialConfig.rxServiceCharUuid]
            val serialWrite = characteristicsByUuid[config.serialConfig.txServiceCharUuid]

            if (serialRead != null && serialWrite != null) {
                peripheral.setNotifyValue(true, forCharacteristic = serialRead)
                serialWriteUpdater(serialWrite)
                info { "Serial characteristics ready (read/write) id=${identifierProvider()}" }
            } else {
                error { "Serial characteristics not found id=${identifierProvider()}" }
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
}
