package net.flipper.bridge.connection.feature.wifi.api

import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiNetwork
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity

interface FWiFiFeatureApi : FDeviceFeatureApi {
    suspend fun getWifiStateFlow(): Flow<ImmutableList<WiFiNetwork>>

    suspend fun connect(
        ssid: String,
        password: String,
        security: WiFiSecurity.Supported
    ): Result<Unit>
}
