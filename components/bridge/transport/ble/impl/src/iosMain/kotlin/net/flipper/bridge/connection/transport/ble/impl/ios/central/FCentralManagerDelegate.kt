package net.flipper.bridge.connection.transport.ble.impl.ios.central

import kotlinx.cinterop.ObjCSignatureOverride
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBManagerState
import platform.CoreBluetooth.CBPeripheral
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.darwin.NSObject

internal class FCentralManagerDelegate(
    private val onStateUpdate: (CBManagerState) -> Unit,
    private val onDidConnect: (CBPeripheral) -> Unit,
    private val onDidDisconnect: (CBPeripheral, NSError?) -> Unit,
    private val onDidFailToConnect: (CBPeripheral, NSError?) -> Unit,
    private val onDidDiscover: (CBPeripheral, Map<Any?, *>, NSNumber) -> Unit
) : NSObject(), CBCentralManagerDelegateProtocol {

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        println(
            "[FCentralManagerDelegate] centralManagerDidUpdateState" +
                " raw=${central.state} authorization=${CBCentralManager.authorization}"
        )
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
