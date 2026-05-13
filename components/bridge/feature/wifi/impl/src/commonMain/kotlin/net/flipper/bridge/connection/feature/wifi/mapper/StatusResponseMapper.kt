package net.flipper.bridge.connection.feature.wifi.mapper

import net.flipper.bridge.connection.feature.rpc.api.model.StatusResponse
import net.flipper.bridge.connection.feature.rpc.api.model.WifiIpMethod
import net.flipper.bridge.connection.feature.rpc.api.model.WifiIpType
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiIpMethod
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiIpType
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiStatus

internal fun StatusResponse.toBsbWifiStatusResponse(): BsbWifiStatus {
    return when (state) {
        StatusResponse.State.UNKNOWN -> BsbWifiStatus.Unknown
        StatusResponse.State.DISCONNECTED -> BsbWifiStatus.Disconnected
        StatusResponse.State.CONNECTING -> BsbWifiStatus.Connecting
        StatusResponse.State.DISCONNECTING -> BsbWifiStatus.Disconnecting
        StatusResponse.State.RECONNECTING -> BsbWifiStatus.Reconnecting
        StatusResponse.State.CONNECTED -> BsbWifiStatus.Connected(
            ssid = ssid,
            bssid = bssid,
            channel = channel,
            rssi = rssi,
            ipConfig = getIpConfig(),
            security = security?.toBsbWifiSecurityMethod()
        )
    }
}

private fun StatusResponse.getIpConfig(): BsbWifiStatus.Connected.BsbWifiIpConfig {
    return BsbWifiStatus.Connected.BsbWifiIpConfig(
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
}
