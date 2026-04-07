package net.flipper.bridge.connection.feature.wifi.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BsbWifiIpType {
    @SerialName("ipv4")
    IPV4,

    @SerialName("ipv6")
    IPV6
}
