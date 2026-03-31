package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_State.Wifi
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent

object WifiProtobufMapper {
    fun map(wifi: Wifi): BusyLibUpdateEvent.Wifi {
        val connected = wifi.connected
        return BusyLibUpdateEvent.Wifi(
            isConnected = connected != null,
            ssid = connected?.ssid,
        )
    }
}
