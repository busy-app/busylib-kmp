package net.flipper.bridge.connection.feature.wifi.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.bridge.connection.feature.wifi.api.serialization.WiFiSecurityPasswordSerializer

@Serializable
sealed interface WiFiSecurity {
    @Serializable
    sealed interface Supported : WiFiSecurity {
        @Serializable
        data object None : Supported

        @Serializable(with = WiFiSecurityPasswordSerializer::class)
        @Suppress("SerialNameNotProvidedRule")
        enum class Password(
            val internalWifiSecurity: BsbWifiSecurityMethod
        ) : Supported {
            WEP(BsbWifiSecurityMethod.WEP),
            WPA(BsbWifiSecurityMethod.WPA),
            WPA2(BsbWifiSecurityMethod.WPA2),
            WPA_WPA2(BsbWifiSecurityMethod.WPA_WPA2),
            WPA3(BsbWifiSecurityMethod.WPA3),
            WPA2_WPA3(BsbWifiSecurityMethod.WPA2_WPA3),
        }
    }

    @Serializable
    data class Other(
        @SerialName("internal_wifi_security")
        val internalWifiSecurity: BsbWifiSecurityMethod
    ) : WiFiSecurity
}
