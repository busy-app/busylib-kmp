package com.flipperdevices.transport.ble.impl

import com.flipperdevices.bridge.connection.transport.ble.api.FBleApi
import com.flipperdevices.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import com.flipperdevices.bridge.connection.transport.ble.api.FSerialBleApi
import com.flipperdevices.bridge.connection.transport.ble.http.FHttpBLEEngine
import com.flipperdevices.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import com.flipperdevices.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import com.flipperdevices.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import com.flipperdevices.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import com.flipperdevices.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import com.flipperdevices.core.busylib.ktx.common.FlipperDispatchers
import com.flipperdevices.transport.ble.impl.cb.FPeripheralApi
import com.flipperdevices.transport.ble.impl.cb.FPeripheralState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus

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
