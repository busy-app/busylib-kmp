package net.flipper.bridge.connection.feature.wifi.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.flipper.bridge.connection.feature.rpc.api.model.WifiSecurityMethod

@Serializable
sealed interface WiFiSecurity {
    @Serializable
    sealed interface Supported : WiFiSecurity {
        @Serializable
        data object None : Supported

        @Serializable
        enum class Password(@Transient val internalWifiSecurity: WifiSecurityMethod) : Supported {
            @SerialName("WEP")
            WEP(WifiSecurityMethod.WEP),

            @SerialName("WPA")
            WPA(WifiSecurityMethod.WPA),

            @SerialName("WPA2")
            WPA2(WifiSecurityMethod.WPA2),

            @SerialName("WPA_WPA2")
            WPA_WPA2(WifiSecurityMethod.WPA_WPA2),

            @SerialName("WPA3")
            WPA3(WifiSecurityMethod.WPA3),

            @SerialName("WPA2_WPA3")
            WPA2_WPA3(WifiSecurityMethod.WPA2_WPA3);

            companion object {
                fun fromInternal(internalWifiSecurity: WifiSecurityMethod): Password? {
                    return entries.firstOrNull { it.internalWifiSecurity == internalWifiSecurity }
                }
            }
        }
    }

    @Serializable
    data class Other(
        @SerialName("internal_wifi_security")
        val internalWifiSecurity: WifiSecurityMethod
    ) : WiFiSecurity
}
