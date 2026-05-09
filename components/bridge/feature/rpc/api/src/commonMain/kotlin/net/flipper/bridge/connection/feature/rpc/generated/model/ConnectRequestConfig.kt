package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConnectRequestConfig(
    @SerialName("ssid")
    val ssid: kotlin.String? = null,
    @SerialName("password")
    val password: kotlin.String? = null,
    @SerialName("security")
    val security: WifiSecurityMethod? = null,
    @SerialName("ip_config")
    val ipConfig: ConnectRequestConfigIpConfig? = null
)
