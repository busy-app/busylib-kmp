package net.flipper.bridge.connection.transport.ble.impl.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import net.flipper.bridge.connection.transport.ble.api.FBleApi
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.ble.api.FSerialBleApi
import net.flipper.bridge.connection.transport.ble.http.FHttpBLEEngine
import net.flipper.bridge.connection.transport.ble.impl.meta.FTransportMetaInfoApiImpl
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import no.nordicsemi.kotlin.ble.client.RemoteService
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.ble.core.BondState
import no.nordicsemi.kotlin.ble.core.ConnectionState

class FAndroidBleApiImpl(
    private val peripheral: Peripheral,
    private val scope: CoroutineScope,
    private val listener: FTransportConnectionStatusListener,
    services: WrappedStateFlow<List<RemoteService>?>,
    serialApi: FSerialBleApi,
    private var currentConfig: FBleDeviceConnectionConfig,
) : FBleApi,
    FHTTPDeviceApi,
    FTransportMetaInfoApi by FTransportMetaInfoApiImpl(services, currentConfig.metaInfoGattMap),
    LogTagProvider {
    override val TAG = "FBleApi"
    private val bleHttpEngine = FHttpBLEEngine(serialApi)

    private fun startPeripheralStatusCollectionJob() {
        combine(
            flow = peripheral.state,
            flow2 = peripheral.bondState,
            transform = { state, bondState ->
                when (state) {
                    ConnectionState.Connected -> when (bondState) {
                        BondState.NONE,
                        BondState.BONDING -> FInternalTransportConnectionStatus.Pairing

                        BondState.BONDED -> FInternalTransportConnectionStatus.Connected(
                            scope = scope,
                            deviceApi = this@FAndroidBleApiImpl,
                            connectionType = FInternalTransportConnectionType.BLE
                        )
                    }

                    ConnectionState.Connecting -> FInternalTransportConnectionStatus.Connecting
                    is ConnectionState.Disconnected -> FInternalTransportConnectionStatus.Disconnected
                    ConnectionState.Disconnecting -> FInternalTransportConnectionStatus.Disconnecting
                }
            }
        ).onEach { status ->
            info { "New status: $status" }
            listener.onStatusUpdate(status)
        }.launchIn(scope)
    }

    init {
        startPeripheralStatusCollectionJob()
    }

    override val deviceName = peripheral.name ?: currentConfig.deviceName

    override suspend fun tryUpdateConnectionConfig(config: FDeviceConnectionConfig<*>): Result<Unit> {
        if (config !is FBleDeviceConnectionConfig) {
            return Result.failure(IllegalArgumentException("Config $config has different type"))
        }
        if (currentConfig == config) {
            return Result.success(Unit)
        }
        if (currentConfig.copy(deviceName = config.deviceName) == config) {
            currentConfig = config
            return Result.success(Unit)
        }
        return Result.failure(IllegalArgumentException("Config $config has different non-name fields"))
    }

    override fun getDeviceHttpEngine() = bleHttpEngine

    private val _capabilities = flowOf(
        listOf(
            FHTTPTransportCapability.BLE_ONLY_CONNECTION_SUPPORTED,
        )
    ).shareIn(scope, SharingStarted.WhileSubscribed(), 1)

    override fun getCapabilities(): Flow<List<FHTTPTransportCapability>> {
        return _capabilities
    }

    override suspend fun disconnect() {
        peripheral.disconnect()
        bleHttpEngine.close()
    }
}
