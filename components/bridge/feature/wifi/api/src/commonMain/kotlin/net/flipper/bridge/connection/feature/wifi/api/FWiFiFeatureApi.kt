package net.flipper.bridge.connection.feature.wifi.api

import kotlinx.collections.immutable.ImmutableList
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.WifiStatusResponse
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiNetwork
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FWiFiFeatureApi : FDeviceFeatureApi {
    fun getWifiStateFlow(): WrappedFlow<ImmutableList<WiFiNetwork>>

    fun getWifiStatusFlow(): WrappedFlow<WifiStatusResponse>

    suspend fun connect(
        ssid: String,
        password: String,
        security: WiFiSecurity.Supported
    ): Result<Unit>
}
