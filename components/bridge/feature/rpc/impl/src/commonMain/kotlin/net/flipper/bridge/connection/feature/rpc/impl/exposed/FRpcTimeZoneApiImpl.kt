package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcTimeZoneApi
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.bridge.connection.feature.rpc.api.model.TimestampInfo
import net.flipper.bridge.connection.feature.rpc.api.model.TimezoneInfo
import net.flipper.bridge.connection.feature.rpc.api.model.TimezoneListResponse
import net.flipper.core.busylib.ktx.common.cache.ObjectCache
import net.flipper.core.busylib.ktx.common.cache.getOrElse
import net.flipper.core.busylib.ktx.common.runSuspendCatching

class FRpcTimeZoneApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher,
    private val objectCache: ObjectCache
) : FRpcTimeZoneApi {
    override suspend fun getTime(ignoreCache: Boolean): Result<TimestampInfo> {
        return runSuspendCatching(dispatcher) {
            objectCache.getOrElse(ignoreCache) {
                httpClient.get("/api/time").body<TimestampInfo>()
            }
        }
    }

    override suspend fun postTimeTimestamp(timestampInfo: TimestampInfo): Result<Unit> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/time/timestamp") {
                parameter("timestamp", timestampInfo.timestamp)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun getTimeTimezone(ignoreCache: Boolean): Result<TimezoneInfo> {
        return runSuspendCatching(dispatcher) {
            objectCache.getOrElse(ignoreCache) {
                httpClient.get("/api/time/timezone").body<TimezoneInfo>()
            }
        }
    }

    override suspend fun postTimeTimezone(timezoneInfo: TimezoneInfo): Result<Unit> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/time/timezone") {
                this.parameter("timezone", timezoneInfo.timezone)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun getTimeTzList(): Result<TimezoneListResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/time/tzlist").body<TimezoneListResponse>()
        }
    }
}
