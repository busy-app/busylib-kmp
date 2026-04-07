package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_State.DeviceName
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent

object DeviceNameProtobufMapper {
    fun map(deviceName: DeviceName): BusyLibUpdateEvent.DeviceName {
        return BusyLibUpdateEvent.DeviceName(deviceName.name)
    }
}
