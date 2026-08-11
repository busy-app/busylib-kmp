package net.flipper.bridge.connection.feature.wifi.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WiFiNetwork(
    @SerialName("ssid")
    val ssid: String,
    @SerialName("rssi")
    val rssi: Int,
    @SerialName("wifi_security")
    val wifiSecurity: WiFiSecurity
)
