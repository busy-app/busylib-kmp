package net.flipper.bridge.connection.transport.ble.impl.ios.peripheral

import net.flipper.bridge.connection.transport.ble.impl.ios.peripheral.queue.BLEEventQueue
import net.flipper.core.busylib.ktx.common.toByteArray
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import platform.CoreBluetooth.CBCharacteristic
import platform.Foundation.NSError

internal class FPeripheralValueRouter(
    private val gattIO: FPeripheralGattIO,
    private val onError: (NSError) -> Unit,
    private val identifierProvider: () -> String,
    private val bleEventQueue: BLEEventQueue
) : LogTagProvider {

    override val TAG: String = "FPeripheralValueRouter"

    fun didUpdateValue(
        characteristic: CBCharacteristic,
        error: NSError?
    ) {
        val characteristicUUID = characteristic.UUID.toKotlinUUID()
        val data = characteristic.value
        val payload = data?.toByteArray()

        debug {
            "didUpdateValue uuid=$characteristicUUID bytes=${payload?.size ?: 0} " +
                    "hasData=${data != null} error=${error?.localizedDescription} id=${identifierProvider()}"
        }
        if (error != null) {
            gattIO.failRead(characteristicUUID, error)
            error { "#didUpdateValue failed ${error.localizedDescription}" }
            onError(error)
            return
        }

        bleEventQueue.onProcess(characteristicUUID, data)
    }
}
