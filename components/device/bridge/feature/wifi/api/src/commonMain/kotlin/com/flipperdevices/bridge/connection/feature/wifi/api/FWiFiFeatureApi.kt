package com.flipperdevices.bridge.connection.feature.wifi.api

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.wifi.api.model.WiFiNetwork
import com.flipperdevices.bridge.connection.feature.wifi.api.model.WiFiSecurity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

interface FWiFiFeatureApi : FDeviceFeatureApi {
    suspend fun getWifiStateFlow(): Flow<ImmutableList<WiFiNetwork>>

    suspend fun connect(
        ssid: String,
        password: String,
        security: WiFiSecurity.Supported
    ): Result<Unit>
}
