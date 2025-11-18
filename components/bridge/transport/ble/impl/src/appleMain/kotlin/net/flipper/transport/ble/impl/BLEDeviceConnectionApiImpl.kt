package net.flipper.transport.ble.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.ble.api.BleDeviceConnectionApi
import net.flipper.bridge.connection.transport.ble.api.FBleApi
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.ble.common.BleConstants
import net.flipper.bridge.connection.transport.ble.common.exception.FailedConnectToDeviceException
import net.flipper.bridge.connection.transport.ble.common.exception.NoFoundDeviceException
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.orEmpty
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.transport.ble.impl.cb.FBLEStatus
import net.flipper.transport.ble.impl.cb.FCentralManager
import net.flipper.transport.ble.impl.cb.FPeripheralApi
import net.flipper.transport.ble.impl.cb.FPeripheralState
import platform.CoreBluetooth.CBCentralManager
import platform.Foundation.NSUUID
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import kotlin.time.Duration

@Inject
@ContributesBinding(BusyLibGraph::class, BleDeviceConnectionApi::class)
class BLEDeviceConnectionApiImpl(
    val manager: CBCentralManager
) : BleDeviceConnectionApi, LogTagProvider {
    override val TAG = "BleDeviceConnectionApi"

    private val centralManager by lazy {
        FCentralManager(manager = manager)
    }

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

        val serialApi = FSerialBleApiImpl(
            scope = scope,
            fPeripheralApi = peripheral
        )

        val bleApi = FBleApiImpl(
            serialApi = serialApi,
            config = config,
            peripheral = peripheral,
            scope = scope,
            listener = listener,
        ) {
            centralManager.disconnect(peripheral.identifier)
        }
        return bleApi
    }

    private suspend fun waitForPeripheralConnect(
        config: FBleDeviceConnectionConfig,
        timeout: Duration
    ): FPeripheralApi? = withTimeoutOrNull(timeout) {
        val deviceIdentifier = NSUUID(config.macAddress)
        info { "Waiting for peripheral in connected stream (timeout: ${timeout.inWholeSeconds}s)..." }

        info { "Waiting for BLE to be powered on..." }
        if (centralManager.bleStatusStream.value != FBLEStatus.POWERED_ON) {
            centralManager
                .bleStatusStream
                .first { status -> status == FBLEStatus.POWERED_ON }
        }

        info { "Waiting for previous connections to disconnect..." }
        centralManager.disconnect(deviceIdentifier)
        val existingDevice = centralManager.connectedStream.value[deviceIdentifier]
        if (existingDevice != null) {
            existingDevice.stateStream
                .filter { it == FPeripheralState.DISCONNECTED }
                .first()
            info { "Previous connection disconnected, proceeding with new connection..." }
        }

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
