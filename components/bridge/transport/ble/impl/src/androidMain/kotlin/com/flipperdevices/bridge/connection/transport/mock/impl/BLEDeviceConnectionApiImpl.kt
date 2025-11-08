package com.flipperdevices.bridge.connection.transport.mock.impl

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.flipperdevices.bridge.connection.transport.ble.api.BleDeviceConnectionApi
import com.flipperdevices.bridge.connection.transport.ble.api.FBleApi
import com.flipperdevices.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import com.flipperdevices.bridge.connection.transport.common.api.DeviceConnectionApi
import com.flipperdevices.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import com.flipperdevices.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import com.flipperdevices.bridge.connection.transport.mock.impl.api.FBleApiImpl
import com.flipperdevices.bridge.connection.transport.mock.impl.api.http.serial.SerialApiFactory
import com.flipperdevices.bridge.connection.transport.mock.impl.exception.BLEConnectionPermissionException
import com.flipperdevices.bridge.connection.transport.mock.impl.exception.FailedConnectToDeviceException
import com.flipperdevices.bridge.connection.transport.mock.impl.exception.NoFoundDeviceException
import com.flipperdevices.bridge.connection.transport.mock.impl.utils.BleConstants
import com.flipperdevices.busylib.core.di.BusyLibGraph
import com.flipperdevices.core.busylib.log.LogTagProvider
import com.flipperdevices.core.busylib.log.info
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeout
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.core.Phy

@Inject
@ContributesBinding(BusyLibGraph::class, BleDeviceConnectionApi::class)
class BLEDeviceConnectionApiImpl(
    private val context: Context,
    private val centralManager: CentralManager,
    private val serialApiFactory: SerialApiFactory
) : BleDeviceConnectionApi, LogTagProvider {
    override val TAG = "BleDeviceConnectionApi"

    override suspend fun connect(
        scope: CoroutineScope,
        config: FBleDeviceConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): Result<FBleApi> = runCatching {
        connectUnsafe(scope, config, listener)
    }

    @Suppress("ThrowsCount", "ForbiddenComment")
    private suspend fun connectUnsafe(
        scope: CoroutineScope,
        config: FBleDeviceConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): FBleApi {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            throw BLEConnectionPermissionException()
        }
        listener.onStatusUpdate(FInternalTransportConnectionStatus.Connecting)
        info { "Finding device with ${BleConstants.CONNECT_TIME.inWholeMilliseconds} timeout..." }
        val device = withTimeout(BleConstants.CONNECT_TIME) {
            centralManager.getPeripheralById(config.macAddress)
        } ?: throw NoFoundDeviceException()
        info { "Device found (${device.address}/$device), try to connect..." }
        withTimeout(BleConstants.CONNECT_TIME) {
            centralManager.connect(
                device,
                CentralManager.ConnectionOptions.Direct(
                    timeout = BleConstants.CONNECT_TIME,
                    preferredPhy = listOf(Phy.PHY_LE_2M),
                    automaticallyRequestHighestValueLength = false
                )
            )
        }
        if (!device.isConnected) {
            info { "Device failed to connect, so throw exception" }
            throw FailedConnectToDeviceException()
        }
        listener.onStatusUpdate(FInternalTransportConnectionStatus.Pairing)
        // TODO: Bonding logic

        info { "Request the highest mtu" }
        device.requestHighestValueLength()

        val services = device.services()

        val serialApi = serialApiFactory.build(
            config = config.serialConfig,
            services = services,
            scope = scope
        )

        val bleApi = FBleApiImpl(
            peripheral = device,
            scope = scope,
            services = services,
            serialApi = serialApi,
            config = config,
            listener = listener
        )
        return bleApi
    }
}
