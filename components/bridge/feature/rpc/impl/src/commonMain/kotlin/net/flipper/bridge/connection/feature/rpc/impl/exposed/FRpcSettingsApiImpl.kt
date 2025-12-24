package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcSettingsApi
import net.flipper.bridge.connection.feature.rpc.api.model.AudioVolumeInfo
import net.flipper.bridge.connection.feature.rpc.api.model.BsbBrightness
import net.flipper.bridge.connection.feature.rpc.api.model.DisplayBrightnessInfo
import net.flipper.bridge.connection.feature.rpc.api.model.NameInfo
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.bridge.connection.feature.rpc.impl.util.runSafely
import net.flipper.core.busylib.ktx.common.cache.ObjectCache
import net.flipper.core.busylib.ktx.common.cache.getOrElse

class FRpcSettingsApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher,
    private val objectCache: ObjectCache
) : FRpcSettingsApi {
    override suspend fun getName(ignoreCache: Boolean): Result<NameInfo> {
        return runSafely(dispatcher) {
            objectCache.getOrElse(ignoreCache) {
                httpClient.get("/api/name").body<NameInfo>()
            }
        }
    }

    override suspend fun setName(body: NameInfo): Result<SuccessResponse> {
        return runSafely(dispatcher) {
            httpClient.post("/api/name") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun getDisplayBrightness(ignoreCache: Boolean): Result<DisplayBrightnessInfo> {
        return runSafely(dispatcher) {
            objectCache.getOrElse(ignoreCache) {
                httpClient.get("/api/display/brightness").body<DisplayBrightnessInfo>()
            }
        }
    }

    private fun BsbBrightness.asParameter(): String {
        return when (this) {
            BsbBrightness.Auto -> "auto"
            is BsbBrightness.Number -> "${this.value}"
        }
    }

    override suspend fun setDisplayBrightness(
        front: BsbBrightness,
        back: BsbBrightness
    ): Result<SuccessResponse> {
        return runSafely(dispatcher) {
            httpClient.post("/api/display/brightness") {
                parameter("front", front.asParameter())
                parameter("back", back.asParameter())
            }.body<SuccessResponse>()
        }
    }

    override suspend fun getAudioVolume(ignoreCache: Boolean): Result<AudioVolumeInfo> {
        return runSafely(dispatcher) {
            objectCache.getOrElse(ignoreCache) {
                httpClient.get("/api/audio/volume").body<AudioVolumeInfo>()
            }
        }
    }

    override suspend fun setAudioVolume(volume: Int): Result<SuccessResponse> {
        return runSafely(dispatcher) {
            httpClient.post("/api/audio/volume") {
                parameter("volume", volume)
            }.body<SuccessResponse>()
        }
    }
}
