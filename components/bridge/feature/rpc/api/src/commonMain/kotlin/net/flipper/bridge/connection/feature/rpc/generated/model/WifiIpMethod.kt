package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WifiIpMethod(val value: kotlin.String) {
    @SerialName("dhcp")
    DHCP("dhcp"),

    @SerialName("static")
    STATIC("static")
}
