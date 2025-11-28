package net.flipper.bridge.connection.transport.ble.impl.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.transport.ble.api.FBleApi
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import net.flipper.bridge.connection.transport.ble.api.FSerialBleApi
import net.flipper.bridge.connection.transport.ble.http.FHttpBLEEngine
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.bridge.connection.transport.ble.impl.meta.FTransportMetaInfoApiImpl
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import no.nordicsemi.kotlin.ble.client.RemoteService
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.ble.core.BondState
import no.nordicsemi.kotlin.ble.core.ConnectionState

class FBleApiImpl(
    private val peripheral: Peripheral,
    private val scope: CoroutineScope,
    private val listener: FTransportConnectionStatusListener,
    services: WrappedStateFlow<List<RemoteService>?>,
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

    override val deviceName = peripheral.name ?: config.deviceName

    override fun getDeviceHttpEngine() = bleHttpEngine

    override suspend fun disconnect() {
        peripheral.disconnect()
    }
}
