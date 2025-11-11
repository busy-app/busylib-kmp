package net.flipper.bridge.connection.feature.wifi.api.model

import kotlinx.serialization.Serializable
import net.flipper.bridge.connection.feature.rpc.api.model.WifiSecurityMethod

@Serializable
sealed interface WiFiSecurity {
    @Serializable
    sealed interface Supported : WiFiSecurity {
        @Serializable
        data object None : Supported

        @Serializable
        enum class Password(val internalWifiSecurity: WifiSecurityMethod) : Supported {
            WEP(WifiSecurityMethod.WEP),
            WPA(WifiSecurityMethod.WPA),
            WPA2(WifiSecurityMethod.WPA2),
            WPA_WPA2(WifiSecurityMethod.WPA_WPA2),
            WPA3(WifiSecurityMethod.WPA3),
            WPA2_WPA3(WifiSecurityMethod.WPA2_WPA3);

            companion object {
                fun fromInternal(internalWifiSecurity: WifiSecurityMethod): Password? {
                    return entries.firstOrNull { it.internalWifiSecurity == internalWifiSecurity }
                }
            }
        }
    }

    @Serializable
    data class Other(val internalWifiSecurity: WifiSecurityMethod) : WiFiSecurity
}
