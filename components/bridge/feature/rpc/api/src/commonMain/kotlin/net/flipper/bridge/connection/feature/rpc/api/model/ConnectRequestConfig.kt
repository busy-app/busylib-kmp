package net.flipper.bridge.connection.feature.rpc.api.model

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
    val ipConfig: IpConfig? = null
) {
    @Serializable
    data class IpConfig(
        @SerialName("ip_method")
        val ipMethod: WifiIpMethod,
        @SerialName("address")
        val address: String? = null,
        @SerialName("mask")
        val mask: String? = null,
        @SerialName("gateway")
        val gateway: String? = null
    )
}
