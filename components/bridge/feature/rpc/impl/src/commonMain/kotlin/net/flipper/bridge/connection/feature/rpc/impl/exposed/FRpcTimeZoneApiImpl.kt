package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.generated.api.TimeApi
import net.flipper.bridge.connection.feature.rpc.generated.model.SuccessResponse
import net.flipper.bridge.connection.feature.rpc.generated.model.TimestampInfo
import net.flipper.bridge.connection.feature.rpc.generated.model.TimezoneInfo
import net.flipper.bridge.connection.feature.rpc.generated.model.TimezoneListResponse
import net.flipper.core.busylib.ktx.common.runSuspendCatching

class FRpcTimeZoneApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher,
) : TimeApi {

    override suspend fun getTime(): Result<TimestampInfo> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/time/timezone").body<TimestampInfo>()
        }
    }

    override suspend fun getTimeTimezone(): Result<TimezoneInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun setTimeTimezone(timezone: kotlin.String): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/time/timezone") {
                this.parameter("timezone", timezone)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun getTimeTzlist(): Result<TimezoneListResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/time/tzlist").body<TimezoneListResponse>()
        }
    }

    override suspend fun setTimeTimestamp(timestamp: String): Result<SuccessResponse> {
        TODO("Not yet implemented")
    }
}
