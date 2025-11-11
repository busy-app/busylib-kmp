package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WifiIpType {
    @SerialName("ipv4")
    IPV4,

    @SerialName("ipv6")
    IPV6
}
