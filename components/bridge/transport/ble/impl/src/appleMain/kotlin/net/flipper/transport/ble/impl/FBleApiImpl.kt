package net.flipper.transport.ble.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import net.flipper.bridge.connection.transport.ble.api.FBleApi
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.ble.api.FSerialBleApi
import net.flipper.bridge.connection.transport.ble.http.FHttpBLEEngine
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.transport.ble.impl.cb.FPeripheralApi
import net.flipper.transport.ble.impl.cb.FPeripheralState

class FBleApiImpl(
    serialApi: FSerialBleApi,
    val config: FBleDeviceConnectionConfig,
    val peripheral: FPeripheralApi,
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
                        deviceApi = this
                    )
                }
            }
            .onEach {
                listener.onStatusUpdate(it)
            }
            .launchIn(scope + FlipperDispatchers.default)
    }

    override suspend fun disconnect() {
        onDisconnect()
    }

    override fun get(key: TransportMetaInfoKey): Result<Flow<ByteArray?>> {
        if (config.metaInfoGattMap.containsKey(key)) {
            val flow = peripheral.metaInfoKeysStream.map { metaMap ->
                metaMap[key]?.toByteArray()
            }
            return Result.success(flow)
        }
        return Result.failure(IllegalArgumentException("Key $key is not supported"))
    }
}
