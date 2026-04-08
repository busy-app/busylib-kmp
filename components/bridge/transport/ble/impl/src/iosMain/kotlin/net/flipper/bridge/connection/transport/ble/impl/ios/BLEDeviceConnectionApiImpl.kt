package net.flipper.bridge.connection.transport.ble.impl.ios

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import dev.zacsweers.metro.binding
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import net.flipper.bridge.connection.transport.ble.api.BleDeviceConnectionApi
import net.flipper.bridge.connection.transport.ble.api.FBleApi
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.ble.impl.BleConstants
import net.flipper.bridge.connection.transport.ble.impl.exception.FailedConnectToDeviceException
import net.flipper.bridge.connection.transport.ble.impl.exception.NoFoundDeviceException
import net.flipper.bridge.connection.transport.ble.impl.ios.api.FIOSBleApiImpl
import net.flipper.bridge.connection.transport.ble.impl.ios.central.FBLEStatus
import net.flipper.bridge.connection.transport.ble.impl.ios.central.FCentralManagerApi
import net.flipper.bridge.connection.transport.ble.impl.ios.peripheral.FPeripheralApi
import net.flipper.bridge.connection.transport.ble.impl.ios.peripheral.FPeripheralState
import net.flipper.bridge.connection.transport.ble.impl.ios.serial.FIOSResetSerialBleApiImpl
import net.flipper.bridge.connection.transport.ble.impl.ios.serial.FIOSSerialBleApiImpl
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import platform.Foundation.NSUUID
import kotlin.time.Duration

@Inject
@ContributesBinding(BusyLibGraph::class, binding = binding<BleDeviceConnectionApi>())
class BLEDeviceConnectionApiImpl(
    private val centralManager: FCentralManagerApi
) : BleDeviceConnectionApi, LogTagProvider {
    override val TAG = "BleDeviceConnectionApi"

    override suspend fun connect(
        scope: CoroutineScope,
        config: FBleDeviceConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): Result<FBleApi> = runCatching {
        connectUnsafe(scope, config, listener)
    }

    @Suppress("ThrowsCount")
    private suspend fun connectUnsafe(
        scope: CoroutineScope,
        config: FBleDeviceConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): FBleApi {
        listener.onStatusUpdate(FInternalTransportConnectionStatus.Connecting)
        info { "Starting BLE connect with ${BleConstants.CONNECT_TIME.inWholeMilliseconds}ms timeout..." }

        // Wait for peripheral to connect with timeout (30 seconds)
        val peripheral = waitForPeripheralConnect(
            scope = scope,
            config = config,
            timeout = BleConstants.CONNECT_TIME
        )

        centralManager.stopScan()
        if (peripheral == null) {
            info { "Connection timeout - disconnecting" }
            throw NoFoundDeviceException()
        }

        info { "Peripheral connected successfully!" }
        listener.onStatusUpdate(FInternalTransportConnectionStatus.Pairing)

        val resetApi = FIOSResetSerialBleApiImpl(
            scope = scope,
            fPeripheralApi = peripheral,
            config = config
        )
        val serialApi = FIOSSerialBleApiImpl(
            scope = scope,
            fPeripheralApi = peripheral,
            resetApi = resetApi,
        )

        val bleApi = FIOSBleApiImpl(
            serialApi = serialApi,
            currentConfig = config,
            peripheral = peripheral,
            scope = scope,
            listener = listener,
        ) {
            centralManager.disconnect(peripheral.identifier)
        }
        return bleApi
    }

    private suspend fun waitForPeripheralConnect(
        scope: CoroutineScope,
        config: FBleDeviceConnectionConfig,
        timeout: Duration
    ): FPeripheralApi? = withTimeoutOrNull(timeout) {
        val deviceIdentifier = NSUUID(config.macAddress)
        info { "Waiting for peripheral in connected stream (timeout: ${timeout.inWholeSeconds}s)..." }

        info { "Waiting for BLE to be powered on, current: ${centralManager.bleStatusStream.value}" }
        centralManager
            .bleStatusStream
            .first { status -> status == FBLEStatus.POWERED_ON }

        info { "Waiting for previous connections to disconnect..." }
        centralManager.disconnect(deviceIdentifier)
        val existingDevice = centralManager.connectedStream.value[deviceIdentifier]
        if (existingDevice != null) {
            existingDevice.stateStream
                .filter { it == FPeripheralState.DISCONNECTED }
                .first()
            info { "Previous connection disconnected, proceeding with new connection..." }
        }

        info { "Connecting to peripheral id=${deviceIdentifier.UUIDString}..." }
        centralManager.connect(scope, config)

        val peripheral = centralManager.connectedStream
            .map { it[deviceIdentifier] }
            .filterNotNull()
            .first()

        info { "Found peripheral in connected stream id=${deviceIdentifier.UUIDString}" }

        // Wait for state to become CONNECTED
        peripheral.stateStream
            .first { state ->
                info { "Peripheral state: $state" }

                when (state) {
                    FPeripheralState.CONNECTED -> {
                        info { "Peripheral ready (connected) id=${deviceIdentifier.UUIDString}" }
                        true
                    }

                    FPeripheralState.PAIRING_FAILED,
                    FPeripheralState.INVALID_PAIRING,
                    FPeripheralState.DISCONNECTED -> {
                        info { "Peripheral connection failed with state: $state" }
                        throw FailedConnectToDeviceException()
                    }

                    else -> false
                }
            }

        peripheral
    }
}
