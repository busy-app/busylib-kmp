package com.flipperdevices.bridge.connection.feature.wifi.api.model

import kotlinx.serialization.Serializable

@Serializable
@Suppress("SerialNameNotProvidedRule") // Don't used in API communication, only decompose config
data class WiFiNetwork(
    val ssid: String,
    val rssi: Int,
    val wifiSecurity: WiFiSecurity
)
