package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_State.Wifi
import BSB_State.WifiConnectionStatus
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent

object WifiProtobufMapper {
    fun map(wifi: Wifi): BusyLibUpdateEvent.Wifi {
        val connected = wifi.connected
        return when {
            connected != null -> BusyLibUpdateEvent.Wifi(
                state = connected.status.toState(),
                ssid = connected.ssid,
                bssid = connected.bssid,
                channel = connected.channel,
                rssi = connected.rssi,
            )
            wifi.disconnected != null -> BusyLibUpdateEvent.Wifi(
                state = BusyLibUpdateEvent.Wifi.State.DISCONNECTED,
                ssid = null,
                bssid = null,
                channel = null,
                rssi = null,
            )
            else -> BusyLibUpdateEvent.Wifi(
                state = BusyLibUpdateEvent.Wifi.State.UNKNOWN,
                ssid = null,
                bssid = null,
                channel = null,
                rssi = null,
            )
        }
    }

    private fun WifiConnectionStatus.toState(): BusyLibUpdateEvent.Wifi.State {
        return when (this) {
            WifiConnectionStatus.CONNECTED -> BusyLibUpdateEvent.Wifi.State.CONNECTED
            WifiConnectionStatus.CONNECTING -> BusyLibUpdateEvent.Wifi.State.CONNECTING
            WifiConnectionStatus.DISCONNECTING -> BusyLibUpdateEvent.Wifi.State.DISCONNECTING
            WifiConnectionStatus.RECONNECTING -> BusyLibUpdateEvent.Wifi.State.RECONNECTING
            else -> BusyLibUpdateEvent.Wifi.State.UNKNOWN
        }
    }
}
