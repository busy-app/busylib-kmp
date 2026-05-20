package net.flipper.bridge.connection.transport.ble.impl

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeout
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.ble.api.BleDeviceConnectionApi
import net.flipper.bridge.connection.transport.ble.api.FBleApi
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.ble.impl.BleConstants.CONNECT_TIME
import net.flipper.bridge.connection.transport.ble.impl.api.FAndroidBleApiImpl
import net.flipper.bridge.connection.transport.ble.impl.api.serial.SerialApiFactory
import net.flipper.bridge.connection.transport.ble.impl.api.stream.AndroidStreamApiFactory
import net.flipper.bridge.connection.transport.ble.impl.exception.BLEConnectionPermissionException
import net.flipper.bridge.connection.transport.ble.impl.exception.FailedConnectToDeviceException
import net.flipper.bridge.connection.transport.ble.impl.exception.NoFoundDeviceException
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.mapSuspendCatching
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import no.nordicsemi.kotlin.ble.client.RemoteService
import no.nordicsemi.kotlin.ble.client.RemoteServices
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.Peripheral
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
    ): Result<FBleApi> = runSuspendCatching {
        withTimeout(CONNECT_TIME) {
            connectUnsafe(scope, config, listener)
        }
    }

    @Suppress("ThrowsCount", "LongMethod")
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
        listener.onStatusUpdate(FInternalTransportConnectionStatus.Connecting(config.getTransportTypes()))
        info { "Finding device with ${CONNECT_TIME.inWholeMilliseconds} timeout..." }
        val device = centralManager.getPeripheralById(config.macAddress)
            ?: throw NoFoundDeviceException()
        info { "Device found (${device.address}/$device), try to connect..." }
        centralManager.connect(
            device,
            CentralManager.ConnectionOptions.Direct(
                timeout = CONNECT_TIME,
                preferredPhy = listOf(Phy.PHY_LE_2M),
                automaticallyRequestHighestValueLength = false
            )
        )
        info { "Device connected!" }
        if (!device.isConnected) {
            info { "Device failed to connect, so throw exception" }
            throw FailedConnectToDeviceException()
        }
        listener.onStatusUpdate(FInternalTransportConnectionStatus.Connecting(config.getTransportTypes()))

        if (!device.hasBondInformation) {
            device.createBond()
            info { "Create bond with device" }
        }

        info { "Request the highest mtu" }
        device.requestHighestValueLength()

        info { "Wait for service discovered" }
        val services = discoverServices(scope, device)
        info { "Services discovered: ${services.value.map { it.uuid }}" }

        val serialApi = serialApiFactory.build(
            config = config.serialConfig,
            services = services,
            scope = scope,
            onResetServices = {
                info { "Requested refresh cache" }
                device.refreshCache() }
        )
        info { "Created serial api" }
        val streamingApi = AndroidStreamApiFactory.buildStreamingApi(
            config.statusStreamingConfig,
            services,
            scope
        )
        val bleApi = FAndroidBleApiImpl(
            peripheral = device,
            scope = scope,
            services = services,
            serialApi = serialApi,
            currentConfig = config,
            listener = listener,
            streamingApi = streamingApi
        )
        info { "Created ble api" }
        return bleApi
    }

    private suspend fun discoverServices(
        scope: CoroutineScope,
        device: Peripheral
    ): StateFlow<List<RemoteService>> {
        info { "Wait for service discovered" }
        val servicesFlow = device.services()
            .onEach {
                debug { "Services state is $it" }
            }

        val discoveredServices = servicesFlow.mapNotNull { state ->
            when (state) {
                is RemoteServices.Failed -> Result.failure(RuntimeException())
                is RemoteServices.Discovered -> Result.success(state.services)

                RemoteServices.Discovering,
                RemoteServices.Unknown -> null
            }
        }.first()
            .getOrNull()

        return if (discoveredServices == null) {
            runSuspendCatching {
                info { "Refreshing GATT cache" }
                device.refreshCache()
            }.mapSuspendCatching {
                info { "GATT cache refreshed" }
                discoverServices(scope, device)
            }.onFailure { error(it) { "Failed to refresh GATT cache, continuing" } }
                .getOrThrow()
        } else {
            servicesFlow
                .filterIsInstance<RemoteServices.Discovered>()
                .map { it.services }
                .stateIn(scope, SharingStarted.Eagerly, discoveredServices)
        }
    }
}
