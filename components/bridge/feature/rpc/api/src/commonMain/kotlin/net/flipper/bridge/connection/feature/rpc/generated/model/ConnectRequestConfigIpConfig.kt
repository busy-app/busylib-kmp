package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConnectRequestConfigIpConfig(
    @SerialName("ip_method")
    val ipMethod: WifiIpMethod? = null,
    @SerialName("ip_type")
    val ipType: IpType? = null,
    @SerialName("address")
    val address: kotlin.String? = null,
    @SerialName("mask")
    val mask: kotlin.String? = null,
    @SerialName("gateway")
    val gateway: kotlin.String? = null
) {

    @Serializable
    enum class IpType(val value: kotlin.String) {
        @SerialName("ipv4")
        IPV4("ipv4"),

        @SerialName("ipv6")
        IPV6("ipv6")
    }
}
