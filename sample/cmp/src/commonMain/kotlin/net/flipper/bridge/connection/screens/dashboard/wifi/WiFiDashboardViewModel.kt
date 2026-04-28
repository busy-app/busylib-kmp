package net.flipper.bridge.connection.screens.dashboard.wifi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.wifi.api.FWiFiFeatureApi
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity
import net.flipper.bridge.connection.screens.dashboard.common.DashboardFeatureViewModel

class WiFiDashboardViewModel(
    private val featureProvider: FFeatureProvider
) : DashboardFeatureViewModel() {
    val networksFlow = featureProvider
        .get(FWiFiFeatureApi::class)
        .getResource { it.getWifiStateFlow() }

    val statusFlow = featureProvider
        .get(FWiFiFeatureApi::class)
        .getResource { it.getWifiStatusFlow() }

    val editingAllowedFlow = featureProvider
        .get(FWiFiFeatureApi::class)
        .getResource { it.isWifiEditingAllowed }

    private val mutableState = MutableStateFlow(WiFiDashboardState())
    val state: StateFlow<WiFiDashboardState> = mutableState

    fun connectToFirstOpenNetwork() = runAction("wifi connect open") {
        val wifiApi = requireFeature<FWiFiFeatureApi>(featureProvider, "WiFi")
        val networks = networksFlow.value.orEmpty()
        val openNetwork = networks.firstOrNull { it.wifiSecurity == WiFiSecurity.Supported.None }
        require(openNetwork != null) { "No open networks discovered yet" }
        wifiApi.connect(
            ssid = openNetwork.ssid,
            password = "",
            security = WiFiSecurity.Supported.None
        ).getOrThrow()
        mutableState.value = mutableState.value.copy(
            lastConnectSummary = "Connecting to ${openNetwork.ssid} (rssi=${openNetwork.rssi})"
        )
        appendLog("Connect requested for ${openNetwork.ssid}")
    }

    fun disconnect() = runAction("wifi disconnect") {
        val wifiApi = requireFeature<FWiFiFeatureApi>(featureProvider, "WiFi")
        wifiApi.disconnect().getOrThrow()
        mutableState.value = mutableState.value.copy(lastConnectSummary = "Disconnect requested")
        appendLog("Disconnect requested")
    }
}

data class WiFiDashboardState(
    val lastConnectSummary: String? = null
)
