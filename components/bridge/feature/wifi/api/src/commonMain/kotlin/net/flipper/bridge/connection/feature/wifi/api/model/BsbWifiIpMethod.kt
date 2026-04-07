package net.flipper.bridge.connection.feature.wifi.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BsbWifiIpMethod {
    @SerialName("dhcp")
    DHCP,

    @SerialName("static")
    STATIC
}
