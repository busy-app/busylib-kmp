package net.flipper.bridge.connection.feature.wifi.api

import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiNetwork
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FWiFiFeatureApi : FDeviceFeatureApi {
    fun getWifiStateFlow(): WrappedFlow<ImmutableList<WiFiNetwork>>

    suspend fun connect(
        ssid: String,
        password: String,
        security: WiFiSecurity.Supported
    ): Result<Unit>
}
