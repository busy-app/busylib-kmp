package net.flipper.bridge.connection.feature.wifi.mapper

import net.flipper.bridge.connection.feature.rpc.api.model.WifiSecurityMethod
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiSecurityMethod

internal fun WifiSecurityMethod.toBsbWifiSecurityMethod(): BsbWifiSecurityMethod {
    return when (this) {
        WifiSecurityMethod.OPEN -> BsbWifiSecurityMethod.OPEN
        WifiSecurityMethod.WPA -> BsbWifiSecurityMethod.WPA
        WifiSecurityMethod.WPA2 -> BsbWifiSecurityMethod.WPA2
        WifiSecurityMethod.WEP -> BsbWifiSecurityMethod.WEP
        WifiSecurityMethod.WPA_WPA2 -> BsbWifiSecurityMethod.WPA_WPA2
        WifiSecurityMethod.WPA3 -> BsbWifiSecurityMethod.WPA3
        WifiSecurityMethod.WPA2_WPA3 -> BsbWifiSecurityMethod.WPA2_WPA3
        WifiSecurityMethod.UNSUPPORTED -> BsbWifiSecurityMethod.UNSUPPORTED
    }
}

internal fun BsbWifiSecurityMethod.toWifiSecurityMethod(): WifiSecurityMethod {
    return when (this) {
        BsbWifiSecurityMethod.OPEN -> WifiSecurityMethod.OPEN
        BsbWifiSecurityMethod.WPA -> WifiSecurityMethod.WPA
        BsbWifiSecurityMethod.WPA2 -> WifiSecurityMethod.WPA2
        BsbWifiSecurityMethod.WEP -> WifiSecurityMethod.WEP
        BsbWifiSecurityMethod.WPA_WPA2 -> WifiSecurityMethod.WPA_WPA2
        BsbWifiSecurityMethod.WPA3 -> WifiSecurityMethod.WPA3
        BsbWifiSecurityMethod.WPA2_WPA3 -> WifiSecurityMethod.WPA2_WPA3
        BsbWifiSecurityMethod.UNSUPPORTED -> WifiSecurityMethod.UNSUPPORTED
    }
}
