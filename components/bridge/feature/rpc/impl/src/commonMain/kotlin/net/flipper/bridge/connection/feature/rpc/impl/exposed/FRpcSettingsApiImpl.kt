package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.generated.api.SettingsApi
import net.flipper.bridge.connection.feature.rpc.generated.model.AudioVolumeInfo
import net.flipper.bridge.connection.feature.rpc.generated.model.DisplayBrightnessInfo
import net.flipper.bridge.connection.feature.rpc.generated.model.HttpAccessInfo
import net.flipper.bridge.connection.feature.rpc.generated.model.NameInfo
import net.flipper.bridge.connection.feature.rpc.generated.model.SuccessResponse
import net.flipper.core.busylib.ktx.common.runSuspendCatching

class FRpcSettingsApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher,
) : SettingsApi {
    override suspend fun apiNameGet(): Result<NameInfo> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/name").body<NameInfo>()
        }
    }

    override suspend fun apiNamePost(nameInfo: NameInfo): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/name") {
                setBody(nameInfo)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun getDisplayBrightness(): Result<DisplayBrightnessInfo> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/display/brightness").body<DisplayBrightnessInfo>()
        }
    }

    override suspend fun getHttpAccess(): Result<HttpAccessInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun setDisplayBrightness(`value`: String): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/display/brightness") {
                parameter("value", `value`)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun setHttpAccess(
        mode: String,
        key: String?
    ): Result<SuccessResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun getAudioVolume(): Result<AudioVolumeInfo> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/audio/volume").body<AudioVolumeInfo>()
        }
    }

    override suspend fun setAudioVolume(
        volume: kotlin.Double,
        silent: kotlin.Int?
    ): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/audio/volume") {
                parameter("volume", volume)
            }.body<SuccessResponse>()
        }
    }
}
