package net.flipper.bridge.connection.feature.wifi.mapper

import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiIpMethod
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiIpType
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiSecurityMethod
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiStatus

internal fun BusyLibUpdateEvent.Wifi.toBsbWifiStatusResponse(): BsbWifiStatus {
    return when (val localState = state) {
        BusyLibUpdateEvent.Wifi.State.Disconnected -> BsbWifiStatus.Disconnected
        BusyLibUpdateEvent.Wifi.State.Unknown -> BsbWifiStatus.Unknown
        is BusyLibUpdateEvent.Wifi.State.Connected -> when (localState.status) {
            BusyLibUpdateEvent.Wifi.State.Connected.Status.CONNECTING -> BsbWifiStatus.Connecting
            BusyLibUpdateEvent.Wifi.State.Connected.Status.DISCONNECTING -> BsbWifiStatus.Disconnecting
            BusyLibUpdateEvent.Wifi.State.Connected.Status.RECONNECTING -> BsbWifiStatus.Reconnecting
            BusyLibUpdateEvent.Wifi.State.Connected.Status.CONNECTED -> mapConnected(
                state = localState,
                ipAddress = ips.firstOrNull()
            )
        }
    }
}

private fun mapConnected(
    state: BusyLibUpdateEvent.Wifi.State.Connected,
    ipAddress: BusyLibUpdateEvent.Wifi.IpAddress?
): BsbWifiStatus {
    return BsbWifiStatus.Connected(
        ssid = state.ssid,
        bssid = state.bssid,
        channel = state.channel,
        rssi = state.rssi,
        security = state.security.toBsbSecurity(),
        ipConfig = ipAddress.mapConfig()
    )
}

private fun BusyLibUpdateEvent.Wifi.IpAddress?.mapConfig(): BsbWifiStatus.Connected.BsbWifiIpConfig {
    if (this == null) {
        return BsbWifiStatus.Connected.BsbWifiIpConfig(null, null, null)
    }
    return BsbWifiStatus.Connected.BsbWifiIpConfig(
        address = address,
        ipMethod = when (method) {
            BusyLibUpdateEvent.Wifi.IpAddress.IpConfigurationMethod.DHCP -> BsbWifiIpMethod.DHCP
            BusyLibUpdateEvent.Wifi.IpAddress.IpConfigurationMethod.STATIC -> BsbWifiIpMethod.STATIC
        },
        ipType = when (protocol) {
            BusyLibUpdateEvent.Wifi.IpAddress.IpProtocol.IPV4 -> BsbWifiIpType.IPV4
            BusyLibUpdateEvent.Wifi.IpAddress.IpProtocol.IPV6 -> BsbWifiIpType.IPV6
        }
    )
}

private fun BusyLibUpdateEvent.Wifi.State.Connected.Security.toBsbSecurity(): BsbWifiSecurityMethod {
    return when (this) {
        BusyLibUpdateEvent.Wifi.State.Connected.Security.UNKNOWN -> BsbWifiSecurityMethod.UNSUPPORTED
        BusyLibUpdateEvent.Wifi.State.Connected.Security.OPEN -> BsbWifiSecurityMethod.OPEN
        BusyLibUpdateEvent.Wifi.State.Connected.Security.WPA -> BsbWifiSecurityMethod.WPA
        BusyLibUpdateEvent.Wifi.State.Connected.Security.WPA2 -> BsbWifiSecurityMethod.WPA2
        BusyLibUpdateEvent.Wifi.State.Connected.Security.WEP -> BsbWifiSecurityMethod.WEP
        BusyLibUpdateEvent.Wifi.State.Connected.Security.WPA_WPA2 -> BsbWifiSecurityMethod.WPA_WPA2
        BusyLibUpdateEvent.Wifi.State.Connected.Security.WPA3 -> BsbWifiSecurityMethod.WPA3
        BusyLibUpdateEvent.Wifi.State.Connected.Security.WPA2_WPA3 -> BsbWifiSecurityMethod.WPA2_WPA3
    }
}
