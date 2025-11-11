package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WifiIpConfig(
    @SerialName("ip_method")
    val ipMethod: WifiIpMethod,
    @SerialName("ip_type")
    val ipType: WifiIpType? = null,
    @SerialName("address")
    val address: String? = null,
    @SerialName("mask")
    val mask: String? = null,
    @SerialName("gateway")
    val gateway: String? = null
)
