package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WifiSecurityMethod {
    @SerialName("Open")
    OPEN,

    @SerialName("WPA")
    WPA,

    @SerialName("WPA2")
    WPA2,

    @SerialName("WEP")
    WEP,

    @SerialName("WPA/WPA2")
    WPA_WPA2,

    @SerialName("WPA3")
    WPA3,

    @SerialName("WPA2/WPA3")
    WPA2_WPA3
}
