package net.flipper.bridge.connection.feature.events.impl.protomapper.delegates

import BSB_State.Timezone
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent

object TimezoneProtobufMapper {
    fun map(timezone: Timezone): BusyLibUpdateEvent.Timezone {
        return BusyLibUpdateEvent.Timezone(
            name = timezone.name,
            offsetMinutes = timezone.offset,
        )
    }
}
