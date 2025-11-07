package com.flipperdevices.bridge.connection.transport.mock.impl.api

import com.flipperdevices.bridge.connection.transport.ble.api.FBleApi
import com.flipperdevices.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import com.flipperdevices.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import com.flipperdevices.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import com.flipperdevices.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import com.flipperdevices.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import com.flipperdevices.bridge.connection.transport.mock.impl.api.http.FHttpBLEEngine
import com.flipperdevices.bridge.connection.transport.mock.impl.api.http.serial.FSerialBleApi
import com.flipperdevices.bridge.connection.transport.mock.impl.meta.FTransportMetaInfoApiImpl
import com.flipperdevices.core.busylib.log.LogTagProvider
import com.flipperdevices.core.busylib.log.info
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import no.nordicsemi.kotlin.ble.client.RemoteService
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.ble.core.BondState
import no.nordicsemi.kotlin.ble.core.ConnectionState

class FBleApiImpl(
    private val peripheral: Peripheral,
    private val scope: CoroutineScope,
    private val listener: FTransportConnectionStatusListener,
    services: StateFlow<List<RemoteService>?>,
    serialApi: FSerialBleApi,
    config: FBleDeviceConnectionConfig,
) : FBleApi,
    FHTTPDeviceApi,
    FTransportMetaInfoApi by FTransportMetaInfoApiImpl(services, config.metaInfoGattMap),
    LogTagProvider {
    override val TAG = "FBleApi"
    private val bleHttpEngine = FHttpBLEEngine(serialApi)

    init {
        scope.launch {
            combine(
                peripheral.state,
                peripheral.bondState
            ) { state, bondState ->
                when (state) {
                    ConnectionState.Connected -> when (bondState) {
                        BondState.NONE,
                        BondState.BONDING -> FInternalTransportConnectionStatus.Pairing

                        BondState.BONDED -> FInternalTransportConnectionStatus.Connected(
                            scope = scope,
                            deviceApi = this@FBleApiImpl
                        )
                    }

                    ConnectionState.Connecting -> FInternalTransportConnectionStatus.Connecting
                    is ConnectionState.Disconnected -> FInternalTransportConnectionStatus.Disconnected
                    ConnectionState.Disconnecting -> FInternalTransportConnectionStatus.Disconnecting
                }
            }.collect {
                info { "New status: $it" }
                listener.onStatusUpdate(it)
            }
        }
    }

    override fun getDeviceHttpEngine() = bleHttpEngine

    override suspend fun disconnect() {
        peripheral.disconnect()
    }
}
