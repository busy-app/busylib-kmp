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
import net.flipper.bridge.connection.feature.rpc.generated.api.AssetsApi
import net.flipper.bridge.connection.feature.rpc.generated.model.DisplayElements
import net.flipper.bridge.connection.feature.rpc.generated.model.PlayAudio
import net.flipper.bridge.connection.feature.rpc.generated.model.SuccessResponse
import net.flipper.core.busylib.ktx.common.runSuspendCatching

class FRpcAssetsApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher
) : AssetsApi {
    override suspend fun uploadAssetWithAppId(
        applicationName: kotlin.String,
        file: kotlin.String,
        body: kotlin.String
    ): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/assets/upload") {
                parameter("application_name", applicationName)
                parameter("file", file)
                contentType(ContentType.Application.OctetStream)
                setBody(body)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun drawOnDisplay(
        displayElements: DisplayElements
    ): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/display/draw") {
                setBody(displayElements)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun playAudio(playAudio: PlayAudio): Result<SuccessResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun stopAudio(): Result<SuccessResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun clearDisplay(applicationName: String?): Result<SuccessResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAppAssets(
        applicationName: kotlin.String
    ): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.delete("/api/display/draw") {
                parameter("application_name", applicationName)
            }.body<SuccessResponse>()
        }
    }
}
