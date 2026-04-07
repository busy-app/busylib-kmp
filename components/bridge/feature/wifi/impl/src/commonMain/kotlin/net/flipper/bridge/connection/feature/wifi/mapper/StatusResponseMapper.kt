package net.flipper.bridge.connection.feature.wifi.mapper

import net.flipper.bridge.connection.feature.rpc.api.model.StatusResponse
import net.flipper.bridge.connection.feature.rpc.api.model.WifiIpMethod
import net.flipper.bridge.connection.feature.rpc.api.model.WifiIpType
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiIpMethod
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiIpType
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiStatusResponse

internal fun StatusResponse.toBsbWifiStatusResponse(): BsbWifiStatusResponse {
    return BsbWifiStatusResponse(
        state = when (state) {
            StatusResponse.State.UNKNOWN -> BsbWifiStatusResponse.BsbWifiState.UNKNOWN
            StatusResponse.State.DISCONNECTED -> BsbWifiStatusResponse.BsbWifiState.DISCONNECTED
            StatusResponse.State.CONNECTED -> BsbWifiStatusResponse.BsbWifiState.CONNECTED
            StatusResponse.State.CONNECTING -> BsbWifiStatusResponse.BsbWifiState.CONNECTING
            StatusResponse.State.DISCONNECTING -> BsbWifiStatusResponse.BsbWifiState.DISCONNECTING
            StatusResponse.State.RECONNECTING -> BsbWifiStatusResponse.BsbWifiState.RECONNECTING
        },
        ssid = ssid,
        bssid = bssid,
        channel = channel,
        rssi = rssi,
        security = security?.toBsbWifiSecurityMethod(),
        ipConfig = BsbWifiStatusResponse.BsbWifiIpConfig(
            ipMethod = when (ipConfig?.ipMethod) {
                WifiIpMethod.DHCP -> BsbWifiIpMethod.DHCP
                WifiIpMethod.STATIC -> BsbWifiIpMethod.STATIC
                null -> null
            },
            ipType = when (ipConfig?.ipType) {
                WifiIpType.IPV4 -> BsbWifiIpType.IPV4
                WifiIpType.IPV6 -> BsbWifiIpType.IPV6
                null -> null
            },
            address = ipConfig?.address
        )
    )
}
