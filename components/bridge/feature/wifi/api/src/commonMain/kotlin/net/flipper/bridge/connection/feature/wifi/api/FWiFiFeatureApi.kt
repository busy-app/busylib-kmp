package net.flipper.bridge.connection.feature.wifi.api

import kotlinx.collections.immutable.ImmutableList
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiStatusResponse
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiNetwork
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FWiFiFeatureApi : FDeviceFeatureApi {
    fun getWifiStateFlow(): WrappedFlow<ImmutableList<WiFiNetwork>>

    fun getWifiStatusFlow(): WrappedFlow<BsbWifiStatusResponse>

    suspend fun connect(
        ssid: String,
        password: String,
        security: WiFiSecurity.Supported
    ): CResult<Unit>

    /**
     * Wi-Fi cannot be edited when there is no BLE/LAN connection
     */
    val isWifiEditingAllowed: WrappedFlow<Boolean>

    suspend fun disconnect(): CResult<Unit>
}
