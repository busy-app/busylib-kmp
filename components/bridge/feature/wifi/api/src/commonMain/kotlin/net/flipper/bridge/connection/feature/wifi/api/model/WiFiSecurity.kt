package net.flipper.bridge.connection.feature.wifi.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed interface WiFiSecurity {
    @Serializable
    sealed interface Supported : WiFiSecurity {
        @Serializable
        data object None : Supported

        @Serializable
        enum class Password(@Transient val internalWifiSecurity: BsbWifiSecurityMethod) : Supported {
            @SerialName("WEP")
            WEP(BsbWifiSecurityMethod.WEP),

            @SerialName("WPA")
            WPA(BsbWifiSecurityMethod.WPA),

            @SerialName("WPA2")
            WPA2(BsbWifiSecurityMethod.WPA2),

            @SerialName("WPA_WPA2")
            WPA_WPA2(BsbWifiSecurityMethod.WPA_WPA2),

            @SerialName("WPA3")
            WPA3(BsbWifiSecurityMethod.WPA3),

            @SerialName("WPA2_WPA3")
            WPA2_WPA3(BsbWifiSecurityMethod.WPA2_WPA3)
        }
    }

    @Serializable
    data class Other(
        @SerialName("internal_wifi_security")
        val internalWifiSecurity: BsbWifiSecurityMethod
    ) : WiFiSecurity
}
