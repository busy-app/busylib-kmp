package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcTimeZoneApi
import net.flipper.bridge.connection.feature.rpc.api.model.RpcTimestampInfo
import net.flipper.bridge.connection.feature.rpc.api.model.RpcTimezoneInfo
import net.flipper.bridge.connection.feature.rpc.api.model.RpcTimezoneListResponse
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.core.busylib.ktx.common.cache.ObjectCache
import net.flipper.core.busylib.ktx.common.cache.getOrElse
import net.flipper.core.busylib.ktx.common.runSuspendCatching

class FRpcTimeZoneApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher,
    private val objectCache: ObjectCache
) : FRpcTimeZoneApi {
    override suspend fun getTime(ignoreCache: Boolean): Result<RpcTimestampInfo> {
        return runSuspendCatching(dispatcher) {
            objectCache.getOrElse(ignoreCache) {
                httpClient.get("/api/time").body<RpcTimestampInfo>()
            }
        }
    }

    override suspend fun postTimeTimestamp(timestampInfo: RpcTimestampInfo): Result<Unit> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/time/timestamp") {
                parameter("timestamp", timestampInfo.timestamp)
            }.body<SuccessResponse>()
        }.map { }
    }

    override suspend fun getTimeTimezone(ignoreCache: Boolean): Result<RpcTimezoneInfo> {
        return runSuspendCatching(dispatcher) {
            objectCache.getOrElse(ignoreCache) {
                httpClient.get("/api/time/timezone").body<RpcTimezoneInfo>()
            }
        }
    }

    override suspend fun postTimeTimezone(timezoneInfo: RpcTimezoneInfo): Result<Unit> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/time/timezone") {
                this.parameter("timezone", timezoneInfo.timezone)
            }.body<SuccessResponse>()
        }.map { }
    }

    override suspend fun getTimeTzList(): Result<RpcTimezoneListResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/time/tzlist").body<RpcTimezoneListResponse>()
        }
    }
}
