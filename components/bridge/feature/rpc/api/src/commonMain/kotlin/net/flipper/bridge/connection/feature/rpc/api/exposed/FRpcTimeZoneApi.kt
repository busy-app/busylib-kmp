package net.flipper.bridge.connection.feature.rpc.api.exposed

import net.flipper.bridge.connection.feature.rpc.api.model.RpcTimestampInfo
import net.flipper.bridge.connection.feature.rpc.api.model.RpcTimezoneInfo
import net.flipper.bridge.connection.feature.rpc.api.model.RpcTimezoneListResponse

interface FRpcTimeZoneApi {
    suspend fun getTime(ignoreCache: Boolean): Result<RpcTimestampInfo>
    suspend fun postTimeTimestamp(timestampInfo: RpcTimestampInfo): Result<Unit>
    suspend fun getTimeTimezone(ignoreCache: Boolean): Result<RpcTimezoneInfo>
    suspend fun postTimeTimezone(timezoneInfo: RpcTimezoneInfo): Result<Unit>
    suspend fun getTimeTzList(): Result<RpcTimezoneListResponse>
}
