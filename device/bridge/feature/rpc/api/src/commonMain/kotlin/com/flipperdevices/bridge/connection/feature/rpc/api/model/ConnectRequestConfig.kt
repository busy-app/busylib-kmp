package com.flipperdevices.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConnectRequestConfig(
    @SerialName("ssid")
    val ssid: String,
    @SerialName("password")
    val password: String,
    @SerialName("security")
    val security: WifiSecurityMethod,
    @SerialName("ip_config")
    val ipConfig: WifiIpConfig
)
