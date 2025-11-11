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

    @SerialName("WPA (Enterprise)")
    WPA_ENTERPRISE,

    @SerialName("WPA2 (Enterprise)")
    WPA2_ENTERPRISE,

    @SerialName("WPA/WPA2")
    WPA_WPA2,

    @SerialName("WPA3")
    WPA3,

    @SerialName("WPA2/WPA3")
    WPA2_WPA3,

    @SerialName("WPA3 (Enterprise)")
    WPA3_ENTERPRISE,

    @SerialName("WPA2/WPA3 (Enterprise)")
    WPA2_WPA3_ENTERPRISE
}
