package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WifiIpType(val value: kotlin.String) {
    @SerialName("ipv4")
    IPV4("ipv4"),

    @SerialName("ipv6")
    IPV6("ipv6")
}
