package net.flipper.bridge.connection.feature.wifi.mapper

import net.flipper.bridge.connection.feature.rpc.generated.model.WifiSecurityMethod
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiSecurityMethod
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity

internal fun WiFiSecurity.Supported.toBsbWifiSecurityMethod(): BsbWifiSecurityMethod {
    return when (this) {
        WiFiSecurity.Supported.None -> BsbWifiSecurityMethod.OPEN
        is WiFiSecurity.Supported.Password -> internalWifiSecurity
    }
}

internal fun WifiSecurityMethod.toWiFiSecurity(): WiFiSecurity {
    val fWifiSecurityMethod = this.toBsbWifiSecurityMethod()
    return when (this) {
        WifiSecurityMethod.OPEN -> WiFiSecurity.Supported.None
        else ->
            WiFiSecurity.Supported.Password
                .entries
                .firstOrNull { password -> password.internalWifiSecurity == fWifiSecurityMethod }
                ?: WiFiSecurity.Other(fWifiSecurityMethod)
    }
}
