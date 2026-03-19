package net.flipper.bridge.connection.transport.ble.impl.ios.peripheral

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBPeripheralDelegateProtocol
import platform.CoreBluetooth.CBService
import platform.Foundation.NSError
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
internal class FPeripheralDelegate(
    val didDiscoverServices: (CBPeripheral, NSError?) -> Unit,
    val didDiscoverCharacteristics: (CBService, NSError?) -> Unit,
    val didUpdateValueForCharacteristic: (CBCharacteristic, NSError?) -> Unit,
    val didWriteValueForCharacteristic: (CBCharacteristic, NSError?) -> Unit,
) : NSObject(), CBPeripheralDelegateProtocol {

    @ObjCSignatureOverride
    override fun peripheral(
        peripheral: CBPeripheral,
        didDiscoverServices: NSError?
    ) {
        didDiscoverServices(peripheral, didDiscoverServices)
    }

    @ObjCSignatureOverride
    override fun peripheral(
        peripheral: CBPeripheral,
        didDiscoverCharacteristicsForService: CBService,
        error: NSError?
    ) {
        didDiscoverCharacteristics(didDiscoverCharacteristicsForService, error)
    }

    @ObjCSignatureOverride
    override fun peripheral(
        peripheral: CBPeripheral,
        didUpdateValueForCharacteristic: CBCharacteristic,
        error: NSError?
    ) {
        didUpdateValueForCharacteristic(didUpdateValueForCharacteristic, error)
    }

    @ObjCSignatureOverride
    override fun peripheral(
        peripheral: CBPeripheral,
        didWriteValueForCharacteristic: CBCharacteristic,
        error: NSError?
    ) {
        didWriteValueForCharacteristic(didWriteValueForCharacteristic, error)
    }
}
