package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_State.Timezone
import kotlinx.datetime.UtcOffset
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent

object TimezoneProtobufMapper {
    fun map(timezone: Timezone): BusyLibUpdateEvent.Timezone? {
        return BusyLibUpdateEvent.Timezone(
            name = timezone.name,
            offset = UtcOffset(minutes = timezone.offset),
            abbreviation = timezone.abbr.ifEmpty { null } ?: return null,
        )
    }
}
