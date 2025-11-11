package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WifiStatusResponse(
    @SerialName("state")
    val state: WifiState,
    @SerialName("ssid")
    val ssid: String? = null,
    @SerialName("security")
    val security: WifiSecurityMethod? = null,
    @SerialName("ip_config")
    val ipConfig: WifiStatusIpConfig? = null
)
