package com.flipperdevices.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Network(
    @SerialName("ssid")
    val ssid: String,
    @SerialName("security")
    val security: WifiSecurityMethod,
    @SerialName("rssi")
    val rssi: Int
)
