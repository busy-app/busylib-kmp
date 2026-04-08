package net.flipper.bridge.connection.feature.timezone.api

import kotlinx.datetime.UtcOffset
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.rpc.api.model.RpcTimezoneInfo
import net.flipper.bridge.connection.feature.rpc.api.model.RpcTimezoneListResponse
import net.flipper.bridge.connection.feature.timezone.api.model.TimezoneInfo

fun RpcTimezoneInfo.toEvent(): BusyLibUpdateEvent.Timezone {
    return BusyLibUpdateEvent.Timezone(
        name = name,
        abbreviation = abbr,
        offset = UtcOffset.parse(offset),
    )
}

fun RpcTimezoneListResponse.toPublic(): List<TimezoneInfo> {
    return list.map { TimezoneInfo(name = it.name, offset = it.offset, abbr = it.abbr) }
}

fun TimezoneInfo.toInternal(): RpcTimezoneInfo {
    return RpcTimezoneInfo(
        name = name,
        abbr = abbr,
        offset = offset
    )
}
