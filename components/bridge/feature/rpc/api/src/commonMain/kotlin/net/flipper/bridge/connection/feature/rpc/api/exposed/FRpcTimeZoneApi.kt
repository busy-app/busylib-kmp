package net.flipper.bridge.connection.feature.rpc.api.exposed

import net.flipper.bridge.connection.feature.rpc.api.model.TimestampInfo
import net.flipper.bridge.connection.feature.rpc.api.model.TimezoneInfo
import net.flipper.bridge.connection.feature.rpc.api.model.TimezoneListResponse

interface FRpcTimeZoneApi {
    suspend fun getTime(ignoreCache: Boolean): Result<TimestampInfo>
    suspend fun postTimeTimestamp(timestampInfo: TimestampInfo): Result<Unit>
    suspend fun getTimeTimezone(ignoreCache: Boolean): Result<TimezoneInfo>
    suspend fun postTimeTimezone(timezoneInfo: TimezoneInfo): Result<Unit>
    suspend fun getTimeTzList(): Result<TimezoneListResponse>
}
