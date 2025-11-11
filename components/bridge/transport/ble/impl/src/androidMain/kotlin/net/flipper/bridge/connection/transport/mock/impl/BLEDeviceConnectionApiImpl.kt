package net.flipper.bridge.connection.transport.mock.impl

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeout
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.ble.api.BleDeviceConnectionApi
import net.flipper.bridge.connection.transport.ble.api.FBleApi
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.ble.common.BleConstants
import net.flipper.bridge.connection.transport.ble.common.exception.BLEConnectionPermissionException
import net.flipper.bridge.connection.transport.ble.common.exception.FailedConnectToDeviceException
import net.flipper.bridge.connection.transport.ble.common.exception.NoFoundDeviceException
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.mock.impl.api.FBleApiImpl
import net.flipper.bridge.connection.transport.mock.impl.api.http.serial.SerialApiFactory
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.core.Phy
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

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

    @Suppress("ThrowsCount")
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
        info { "Device connected!" }
        if (!device.isConnected) {
            info { "Device failed to connect, so throw exception" }
            throw FailedConnectToDeviceException()
        }
        listener.onStatusUpdate(FInternalTransportConnectionStatus.Pairing)

        if (!device.hasBondInformation) {
            device.createBond()
            info { "Create bond with device" }
        }

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
