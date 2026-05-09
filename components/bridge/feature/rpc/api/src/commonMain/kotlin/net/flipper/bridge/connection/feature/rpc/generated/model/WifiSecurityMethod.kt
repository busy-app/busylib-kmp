package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WifiSecurityMethod(val value: kotlin.String) {
    @SerialName("Open")
    OPEN("Open"),

    @SerialName("WPA")
    WPA("WPA"),

    @SerialName("WPA2")
    WPA2("WPA2"),

    @SerialName("WEP")
    WEP("WEP"),

    @SerialName("WPA/WPA2")
    WPA_SLASH_WPA2("WPA/WPA2"),

    @SerialName("WPA3")
    WPA3("WPA3"),

    @SerialName("WPA2/WPA3")
    WPA2_SLASH_WPA3("WPA2/WPA3"),

    @SerialName("Unsupported")
    UNSUPPORTED("Unsupported")
}
