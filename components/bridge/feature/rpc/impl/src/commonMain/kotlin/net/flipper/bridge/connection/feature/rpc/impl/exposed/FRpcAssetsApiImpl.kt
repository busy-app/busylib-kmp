package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcAssetsApi
import net.flipper.bridge.connection.feature.rpc.api.model.DrawRequest
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.core.busylib.ktx.common.runSuspendCatching

class FRpcAssetsApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher
) : FRpcAssetsApi {
    override suspend fun uploadAsset(
        appId: String,
        file: String,
        content: ByteArray
    ): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/assets/upload") {
                parameter("app_id", appId)
                parameter("file", file)
                contentType(ContentType.Application.OctetStream)
                setBody(content)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun displayDraw(
        request: DrawRequest
    ): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/display/draw") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun removeDraw(
        appId: String
    ): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.delete("/api/display/draw") {
                parameter("app_id", appId)
            }.body<SuccessResponse>()
        }
    }
}
