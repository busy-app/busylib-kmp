package net.flipper.bridge.connection.feature.timezone.api

import net.flipper.bridge.connection.feature.rpc.api.model.RpcTimestampInfo
import net.flipper.bridge.connection.feature.rpc.api.model.RpcTimezoneInfo
import net.flipper.bridge.connection.feature.rpc.api.model.RpcTimezoneListResponse
import net.flipper.bridge.connection.feature.timezone.api.model.TimestampInfo
import net.flipper.bridge.connection.feature.timezone.api.model.TimezoneInfo

fun RpcTimezoneInfo.toPublic(): TimezoneInfo {
    return TimezoneInfo(
        name = name,
        abbr = abbr,
        offset = offset
    )
}

fun RpcTimestampInfo.toPublic(): TimestampInfo {
    return TimestampInfo(timestamp = timestamp)
}

fun RpcTimezoneListResponse.toPublic(): List<TimezoneInfo> {
    return list.map { TimezoneInfo(name = it.name, offset = it.offset, abbr = it.abbr) }
}

fun TimestampInfo.toInternal(): RpcTimestampInfo {
    return RpcTimestampInfo(timestamp = timestamp)
}

fun TimezoneInfo.toInternal(): RpcTimezoneInfo {
    return RpcTimezoneInfo(
        name = name,
        abbr = abbr,
        offset = offset
    )
}
