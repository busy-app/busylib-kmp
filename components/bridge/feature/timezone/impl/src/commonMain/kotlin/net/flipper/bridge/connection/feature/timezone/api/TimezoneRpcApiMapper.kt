package net.flipper.bridge.connection.feature.timezone.api

import net.flipper.bridge.connection.feature.rpc.api.model.RpcTimestampInfo
import net.flipper.bridge.connection.feature.rpc.api.model.RpcTimezoneInfo
import net.flipper.bridge.connection.feature.rpc.api.model.RpcTimezoneListResponse
import net.flipper.bridge.connection.feature.timezone.api.model.TimestampInfo
import net.flipper.bridge.connection.feature.timezone.api.model.TimezoneInfo
import net.flipper.bridge.connection.feature.timezone.api.model.TimezoneListItem

fun RpcTimezoneInfo.toPublic(): TimezoneInfo {
    return TimezoneInfo(timezone = timezone)
}

fun RpcTimestampInfo.toPublic(): TimestampInfo {
    return TimestampInfo(timestamp = timestamp)
}

fun RpcTimezoneListResponse.toPublic(): List<TimezoneListItem> {
    return list.map { TimezoneListItem(name = it.name, offset = it.offset, abbr = it.abbr) }
}

fun TimestampInfo.toInternal(): RpcTimestampInfo {
    return RpcTimestampInfo(timestamp = timestamp)
}

fun TimezoneInfo.toInternal(): RpcTimezoneInfo {
    return RpcTimezoneInfo(timezone = timezone)
}
