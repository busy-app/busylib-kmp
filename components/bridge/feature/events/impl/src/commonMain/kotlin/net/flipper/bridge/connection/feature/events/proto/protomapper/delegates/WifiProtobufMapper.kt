package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_State.Wifi
import BSB_State.WifiConnectionStatus
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiStatusResponse
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiStatusResponse.BsbWifiState

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
                state = BsbWifiState.DISCONNECTED,
                ssid = null,
                bssid = null,
                channel = null,
                rssi = null,
            )
            else -> BusyLibUpdateEvent.Wifi(
                state = BsbWifiState.UNKNOWN,
                ssid = null,
                bssid = null,
                channel = null,
                rssi = null,
            )
        }
    }

    private fun WifiConnectionStatus.toState(): BsbWifiState {
        return when (this) {
            WifiConnectionStatus.CONNECTED -> BsbWifiState.CONNECTED
            WifiConnectionStatus.CONNECTING -> BsbWifiState.CONNECTING
            WifiConnectionStatus.DISCONNECTING -> BsbWifiState.DISCONNECTING
            WifiConnectionStatus.RECONNECTING -> BsbWifiState.RECONNECTING
            else -> BsbWifiState.UNKNOWN
        }
    }
}
