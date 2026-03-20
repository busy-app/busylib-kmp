package net.flipper.bridge.connection.feature.wifi.util

import net.flipper.bridge.connection.feature.rpc.api.model.Network
import net.flipper.bridge.connection.feature.rpc.api.model.WifiSecurityMethod
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiNetwork
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity

internal fun Network.toWiFiNetwork(): WiFiNetwork {
    return WiFiNetwork(
        ssid = ssid,
        rssi = rssi,
        wifiSecurity = when (security) {
            WifiSecurityMethod.OPEN -> WiFiSecurity.Supported.None
            else -> WiFiSecurity.Supported.Password.fromInternal(
                security
            ) ?: WiFiSecurity.Other(security)
        }
    )
}

internal fun WiFiSecurity.Supported.toInternalSecurity(): WifiSecurityMethod {
    return when (this) {
        WiFiSecurity.Supported.None -> WifiSecurityMethod.OPEN
        is WiFiSecurity.Supported.Password -> internalWifiSecurity
    }
}
