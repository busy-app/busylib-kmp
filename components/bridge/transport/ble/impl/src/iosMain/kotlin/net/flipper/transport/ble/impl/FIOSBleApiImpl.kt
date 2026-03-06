package net.flipper.transport.ble.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.plus
import net.flipper.bridge.connection.transport.ble.api.FBleApi
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.ble.api.FSerialBleApi
import net.flipper.bridge.connection.transport.ble.http.FHttpBLEEngine
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoData
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.transport.ble.impl.cb.FPeripheralApi
import net.flipper.transport.ble.impl.cb.FPeripheralState

class FIOSBleApiImpl(
    serialApi: FSerialBleApi,
    private var currentConfig: FBleDeviceConnectionConfig,
    private val peripheral: FPeripheralApi,
    private val scope: CoroutineScope,
    private val listener: FTransportConnectionStatusListener,
    private val onDisconnect: suspend () -> Unit,
) : FBleApi,
    FHTTPDeviceApi,
    FTransportMetaInfoApi {
    private val bleHttpEngine = FHttpBLEEngine(serialApi)

    override fun getDeviceHttpEngine() = bleHttpEngine

    init {
        peripheral
            .stateStream
            .map {
                when (it) {
                    FPeripheralState.CONNECTING -> FInternalTransportConnectionStatus.Connecting
                    FPeripheralState.DISCONNECTING -> FInternalTransportConnectionStatus.Disconnecting
                    FPeripheralState.DISCONNECTED,
                    FPeripheralState.PAIRING_FAILED,
                    FPeripheralState.INVALID_PAIRING -> FInternalTransportConnectionStatus.Disconnected

                    FPeripheralState.CONNECTED -> FInternalTransportConnectionStatus.Connected(
                        scope = scope,
                        deviceApi = this,
                        connectionType = FInternalTransportConnectionType.BLE
                    )
                }
            }
            .onEach {
                listener.onStatusUpdate(it)
            }
            .launchIn(scope + FlipperDispatchers.default)
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

    override suspend fun disconnect() {
        onDisconnect()
    }

    private val _capabilities = flowOf(
        listOf(
            FHTTPTransportCapability.BLE_ONLY_CONNECTION_SUPPORTED,
        )
    ).shareIn(scope, SharingStarted.WhileSubscribed(), 1)

    override fun getCapabilities(): Flow<List<FHTTPTransportCapability>> {
        return _capabilities
    }

    override fun get(key: TransportMetaInfoKey): Flow<Result<Flow<TransportMetaInfoData?>>> {
        if (currentConfig.metaInfoGattMap.containsKey(key)) {
            val innerFlow = peripheral.metaInfoKeysStream.map { metaMap ->
                metaMap[key]?.let { TransportMetaInfoData.RawBytes(it) }
            }
            return flowOf(Result.success(innerFlow))
        }
        return flowOf(Result.failure(IllegalArgumentException("Key $key is not supported")))
    }
}
