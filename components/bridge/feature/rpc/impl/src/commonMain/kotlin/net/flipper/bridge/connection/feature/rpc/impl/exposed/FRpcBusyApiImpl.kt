package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.appendPathSegments
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.generated.api.BusyApi
import net.flipper.bridge.connection.feature.rpc.generated.model.SuccessResponse
import net.flipper.core.busylib.ktx.common.runSuspendCatching

class FRpcBusyApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher
) : BusyApi {

    override suspend fun getBusySnapshot(): Result<String> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/busy/snapshot").bodyAsText()
        }
    }

    override suspend fun setBusySnapshot(rawJson: String): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.put("/api/busy/snapshot") {
                setBody(TextContent(rawJson, ContentType.Application.Json))
            }.body<SuccessResponse>()
        }
    }

    override suspend fun getBusyProfile(slot: String): Result<String> {
        return runSuspendCatching(dispatcher) {
            httpClient.get {
                url {
                    appendPathSegments("api", "busy", "profiles", slot, encodeSlash = true)
                }
            }.bodyAsText()
        }
    }

    override suspend fun setBusyProfile(slot: String, rawJson: String): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.put {
                url {
                    appendPathSegments("api", "busy", "profiles", slot, encodeSlash = true)
                }
                setBody(TextContent(rawJson, ContentType.Application.Json))
            }.body<SuccessResponse>()
        }
    }
}
