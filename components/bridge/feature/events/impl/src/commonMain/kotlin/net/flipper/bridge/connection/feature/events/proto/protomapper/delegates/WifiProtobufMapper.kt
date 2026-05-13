package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_State.IpAddress
import BSB_State.IpConfigurationMethod
import BSB_State.IpProtocol
import BSB_State.Wifi
import BSB_State.WifiConnectionStatus
import BSB_State.WifiSecurity
import BSB_State.WifiStateConnected
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent

object WifiProtobufMapper {
    fun map(wifi: Wifi): BusyLibUpdateEvent.Wifi? {
        val addresses = wifi.ip_addresses.map(::mapIp)
        val connected = wifi.connected
        val state = when {
            connected != null -> mapConnected(connected) ?: return null
            wifi.disconnected != null -> BusyLibUpdateEvent.Wifi.State.Disconnected
            wifi.unknown != null -> BusyLibUpdateEvent.Wifi.State.Unknown
            else -> return null
        }
        return BusyLibUpdateEvent.Wifi(
            state = state,
            ips = addresses
        )
    }

    private fun mapConnected(connected: WifiStateConnected): BusyLibUpdateEvent.Wifi.State.Connected? {
        return BusyLibUpdateEvent.Wifi.State.Connected(
            ssid = connected.ssid,
            bssid = connected.bssid,
            channel = connected.channel,
            rssi = connected.rssi,
            security = connected.security.toState(),
            status = connected.status.toState() ?: return null
        )
    }

    private fun mapIp(ipAddress: IpAddress): BusyLibUpdateEvent.Wifi.IpAddress {
        val protocol = when (ipAddress.protocol) {
            IpProtocol.IPV4 -> BusyLibUpdateEvent.Wifi.IpAddress.IpProtocol.IPV4
            is IpProtocol.Unrecognized,
            IpProtocol.IPV6 -> BusyLibUpdateEvent.Wifi.IpAddress.IpProtocol.IPV6
        }
        val method = when (ipAddress.method) {
            is IpConfigurationMethod.Unrecognized,
            IpConfigurationMethod.DHCP -> BusyLibUpdateEvent.Wifi.IpAddress.IpConfigurationMethod.DHCP

            IpConfigurationMethod.STATIC -> BusyLibUpdateEvent.Wifi.IpAddress.IpConfigurationMethod.STATIC
        }
        return BusyLibUpdateEvent.Wifi.IpAddress(
            protocol = protocol,
            method = method,
            address = ipAddress.address,
            gateway = ipAddress.gateway,
            netmask = ipAddress.netmask
        )
    }

    private fun WifiSecurity.toState(): BusyLibUpdateEvent.Wifi.State.Connected.Security {
        return when (this) {
            WifiSecurity.OPEN -> BusyLibUpdateEvent.Wifi.State.Connected.Security.OPEN
            WifiSecurity.UNKNOWN,
            is WifiSecurity.Unrecognized -> BusyLibUpdateEvent.Wifi.State.Connected.Security.UNKNOWN

            WifiSecurity.WEP -> BusyLibUpdateEvent.Wifi.State.Connected.Security.WEP
            WifiSecurity.WPA -> BusyLibUpdateEvent.Wifi.State.Connected.Security.WPA
            WifiSecurity.WPA2 -> BusyLibUpdateEvent.Wifi.State.Connected.Security.WPA2
            WifiSecurity.WPA2_WPA3 -> BusyLibUpdateEvent.Wifi.State.Connected.Security.WPA2_WPA3
            WifiSecurity.WPA3 -> BusyLibUpdateEvent.Wifi.State.Connected.Security.WPA3
            WifiSecurity.WPA_WPA2 -> BusyLibUpdateEvent.Wifi.State.Connected.Security.WPA_WPA2
        }
    }

    private fun WifiConnectionStatus.toState(): BusyLibUpdateEvent.Wifi.State.Connected.Status? {
        return when (this) {
            WifiConnectionStatus.CONNECTED -> BusyLibUpdateEvent.Wifi.State.Connected.Status.CONNECTED
            WifiConnectionStatus.CONNECTING -> BusyLibUpdateEvent.Wifi.State.Connected.Status.CONNECTING
            WifiConnectionStatus.DISCONNECTING -> BusyLibUpdateEvent.Wifi.State.Connected.Status.DISCONNECTING
            WifiConnectionStatus.RECONNECTING -> BusyLibUpdateEvent.Wifi.State.Connected.Status.RECONNECTING
            is WifiConnectionStatus.Unrecognized -> null
        }
    }
}
