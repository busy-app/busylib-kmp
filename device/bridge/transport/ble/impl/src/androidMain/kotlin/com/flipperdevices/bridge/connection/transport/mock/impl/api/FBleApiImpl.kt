package com.flipperdevices.bridge.connection.transport.mock.impl.api

import com.flipperdevices.bridge.connection.transport.ble.api.FBleApi
import com.flipperdevices.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import com.flipperdevices.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import com.flipperdevices.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import com.flipperdevices.bridge.connection.transport.mock.impl.api.http.FHttpBLEEngine
import com.flipperdevices.bridge.connection.transport.mock.impl.api.http.serial.FSerialBleApi
import com.flipperdevices.bridge.connection.transport.mock.impl.meta.FTransportMetaInfoApiImpl
import kotlinx.coroutines.flow.StateFlow
import no.nordicsemi.kotlin.ble.client.RemoteService

class FBleApiImpl(
    private val onDisconnect: suspend () -> Unit,
    services: StateFlow<List<RemoteService>?>,
    serialApi: FSerialBleApi,
    config: FBleDeviceConnectionConfig,
) : FBleApi,
    FHTTPDeviceApi,
    FTransportMetaInfoApi by FTransportMetaInfoApiImpl(services, config.metaInfoGattMap) {
    private val bleHttpEngine = FHttpBLEEngine(serialApi)

    override fun getDeviceHttpEngine() = bleHttpEngine

    override suspend fun disconnect() {
        onDisconnect()
    }
}
