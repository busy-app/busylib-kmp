package com.flipperdevices.transport.ble.impl

import com.flipperdevices.bridge.connection.transport.ble.api.BleDeviceConnectionApi
import com.flipperdevices.bridge.connection.transport.ble.api.FBleApi
import com.flipperdevices.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import com.flipperdevices.bridge.connection.transport.ble.common.BleConstants
import com.flipperdevices.bridge.connection.transport.ble.common.exception.FailedConnectToDeviceException
import com.flipperdevices.bridge.connection.transport.ble.common.exception.NoFoundDeviceException
import com.flipperdevices.bridge.connection.transport.common.api.DeviceConnectionApi
import com.flipperdevices.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import com.flipperdevices.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import com.flipperdevices.busylib.core.di.BusyLibGraph
import com.flipperdevices.core.busylib.log.LogTagProvider
import com.flipperdevices.core.busylib.log.info
import com.flipperdevices.transport.ble.impl.cb.FCentralManager
import com.flipperdevices.transport.ble.impl.cb.FPeripheralApi
import com.flipperdevices.transport.ble.impl.cb.FPeripheralState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import me.tatarka.inject.annotations.Inject
import platform.Foundation.NSUUID
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import kotlin.time.Duration

@Inject
@ContributesBinding(BusyLibGraph::class, BleDeviceConnectionApi::class)
class BLEDeviceConnectionApiImpl() : BleDeviceConnectionApi, LogTagProvider {
    override val TAG = "BleDeviceConnectionApi"

    private val centralManager by lazy {
        FCentralManager()
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

        // Start connection process
        centralManager.connect(config)

        // Wait for peripheral to connect with timeout (30 seconds)
        val peripheral = waitForPeripheralConnect(
            deviceIdentifier = NSUUID(config.macAddress),
            timeout = BleConstants.CONNECT_TIME
        )

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

    /**
     * Waits for a peripheral to connect with a timeout, similar to Swift implementation.
     * Returns null if timeout occurs.
     */
    private suspend fun waitForPeripheralConnect(
        deviceIdentifier: NSUUID,
        timeout: Duration
    ): FPeripheralApi? = withTimeoutOrNull(timeout) {
        info { "Waiting for peripheral in connected stream (timeout: ${timeout.inWholeSeconds}s)..." }

        // Wait until device appears in connected stream
        val peripheral = centralManager.connectedStream
            .map { it[deviceIdentifier] }
            .filter { it != null }
            .first()!!

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
