package net.flipper.bridge.connection.transport.ble.impl.ios.central

import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.onFailure
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBManagerState
import platform.CoreBluetooth.CBPeripheral
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.darwin.NSObject

internal sealed class FCentralManagerEvent {
    data class StateUpdated(val state: CBManagerState) : FCentralManagerEvent()
    data class DidConnect(val peripheral: CBPeripheral) : FCentralManagerEvent()
    data class DidDisconnect(val peripheral: CBPeripheral, val error: NSError?) : FCentralManagerEvent()
    data class DidFailToConnect(val peripheral: CBPeripheral, val error: NSError?) : FCentralManagerEvent()
    data class DidDiscover(
        val peripheral: CBPeripheral,
        val advertisementData: Map<Any?, *>,
        val rssi: NSNumber
    ) : FCentralManagerEvent()
}

internal class FCentralManagerDelegate(
    private val onError: (FCentralManagerEvent, Throwable) -> Unit = { _, _ -> }
) : NSObject(), CBCentralManagerDelegateProtocol {
    private val _events: Channel<FCentralManagerEvent> = Channel(Channel.UNLIMITED)
    val events = _events as ReceiveChannel<FCentralManagerEvent>

    private fun send(event: FCentralManagerEvent) {
        _events.trySend(event).onFailure { error ->
            if (error != null) onError(event, error)
        }
    }

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        send(FCentralManagerEvent.StateUpdated(central.state))
    }

    override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
        send(FCentralManagerEvent.DidConnect(didConnectPeripheral))
    }

    @ObjCSignatureOverride
    override fun centralManager(
        central: CBCentralManager,
        didDisconnectPeripheral: CBPeripheral,
        error: NSError?
    ) {
        send(FCentralManagerEvent.DidDisconnect(didDisconnectPeripheral, error))
    }

    @ObjCSignatureOverride
    override fun centralManager(
        central: CBCentralManager,
        didFailToConnectPeripheral: CBPeripheral,
        error: NSError?
    ) {
        send(FCentralManagerEvent.DidFailToConnect(didFailToConnectPeripheral, error))
    }

    override fun centralManager(
        central: CBCentralManager,
        didDiscoverPeripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        RSSI: NSNumber
    ) {
        send(FCentralManagerEvent.DidDiscover(didDiscoverPeripheral, advertisementData, RSSI))
    }
}
