package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_State.Matter
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent

object MatterProtobufMapper {
    fun map(matter: Matter): BusyLibUpdateEvent.Matter {
        return BusyLibUpdateEvent.Matter(
            fabricCount = matter.fabric_count,
        )
    }
}
