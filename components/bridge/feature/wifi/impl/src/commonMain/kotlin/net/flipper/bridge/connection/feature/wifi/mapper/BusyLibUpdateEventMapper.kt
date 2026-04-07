package net.flipper.bridge.connection.feature.wifi.mapper

import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiStatusResponse

internal fun BusyLibUpdateEvent.Wifi.State.toStatusResponseState(): BsbWifiStatusResponse.BsbWifiState {
    return when (this) {
        BusyLibUpdateEvent.Wifi.State.UNKNOWN -> BsbWifiStatusResponse.BsbWifiState.UNKNOWN
        BusyLibUpdateEvent.Wifi.State.DISCONNECTED -> BsbWifiStatusResponse.BsbWifiState.DISCONNECTED
        BusyLibUpdateEvent.Wifi.State.CONNECTED -> BsbWifiStatusResponse.BsbWifiState.CONNECTED
        BusyLibUpdateEvent.Wifi.State.CONNECTING -> BsbWifiStatusResponse.BsbWifiState.CONNECTING
        BusyLibUpdateEvent.Wifi.State.DISCONNECTING -> BsbWifiStatusResponse.BsbWifiState.DISCONNECTING
        BusyLibUpdateEvent.Wifi.State.RECONNECTING -> BsbWifiStatusResponse.BsbWifiState.RECONNECTING
    }
}
