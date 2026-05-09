package net.flipper.bridge.connection.feature.wifi.mapper

import net.flipper.bridge.connection.feature.rpc.generated.model.Network
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiNetwork

internal fun Network.toWiFiNetwork(): WiFiNetwork {
    return WiFiNetwork(
        ssid = ssid,
        rssi = rssi,
        wifiSecurity = security.toWiFiSecurity()
    )
}
