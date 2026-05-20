package net.flipper.bridge.connection.transport.ble.impl.ios.peripheral

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.DEVICE_NAME_SHORT_UUID
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.RecordingPeripheral
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.SERIAL_RESET_UUID
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.SERIAL_RX_SHORT_UUID
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.SERIAL_SERVICE_SHORT_UUID
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.SERIAL_TX_SHORT_UUID
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.createConfig
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.newCharacteristic
import net.flipper.bridge.connection.transport.ble.impl.ios.testfixtures.newService
import platform.CoreBluetooth.CBCharacteristic

internal data class Sut(
    val peripheral: RecordingPeripheral,
    val sut: FPeripheral,
    val config: FBleDeviceConnectionConfig,
)

internal data class ConnectedSut(
    val peripheral: RecordingPeripheral,
    val sut: FPeripheral,
    val config: FBleDeviceConnectionConfig,
    val tx: CBCharacteristic,
)

internal fun TestScope.createSut(
    peripheral: RecordingPeripheral = RecordingPeripheral().apply { setStateRaw(0L) },
    config: FBleDeviceConnectionConfig = createConfig(macAddress = peripheral.identifier.UUIDString),
): Sut {
    val sut = FPeripheral(
        peripheral = peripheral,
        config = config,
        scope = backgroundScope,
    )
    return Sut(peripheral = peripheral, sut = sut, config = config)
}

internal suspend fun TestScope.createConnectedSut(
    metaPayload: ByteArray = "ready".encodeToByteArray(),
): ConnectedSut {
    val base = createSut()

    val rx = newCharacteristic(SERIAL_RX_SHORT_UUID)
    val tx = newCharacteristic(SERIAL_TX_SHORT_UUID)
    val reset = newCharacteristic(SERIAL_RESET_UUID)
    val serialService = newService(SERIAL_SERVICE_SHORT_UUID, listOf(rx, tx, reset))
    base.sut.didDiscoverCharacteristics(serialService, error = null)

    val metaChar = newCharacteristic(DEVICE_NAME_SHORT_UUID, payload = metaPayload)
    base.sut.didUpdateValue(metaChar, error = null)
    // BLEEventQueue processes events asynchronously — wait until the meta update
    // is observable so callers can assert against a fully-connected SUT.
    base.sut.stateStream.first { it == FPeripheralState.CONNECTED }
    base.sut.metaInfoKeysStream.first { it.isNotEmpty() }

    return ConnectedSut(
        peripheral = base.peripheral,
        sut = base.sut,
        config = base.config,
        tx = tx,
    )
}
