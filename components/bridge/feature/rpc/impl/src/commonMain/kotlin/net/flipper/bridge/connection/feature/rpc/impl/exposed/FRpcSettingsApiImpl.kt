package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcSettingsApi
import net.flipper.bridge.connection.feature.rpc.api.model.AudioVolumeInfo
import net.flipper.bridge.connection.feature.rpc.api.model.BsbBrightness
import net.flipper.bridge.connection.feature.rpc.api.model.DisplayBrightnessInfo
import net.flipper.bridge.connection.feature.rpc.api.model.NameInfo
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.core.busylib.ktx.common.cache.ObjectCache
import net.flipper.core.busylib.ktx.common.cache.getOrElse
import net.flipper.core.busylib.ktx.common.runSuspendCatching

class FRpcSettingsApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher,
    private val objectCache: ObjectCache
) : FRpcSettingsApi {
    override suspend fun getName(ignoreCache: Boolean): Result<NameInfo> {
        return runSuspendCatching(dispatcher) {
            objectCache.getOrElse(ignoreCache) {
                httpClient.get("/api/name").body<NameInfo>()
            }
        }
    }

    override suspend fun setName(body: NameInfo): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/name") {
                setBody(body)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun getDisplayBrightness(ignoreCache: Boolean): Result<DisplayBrightnessInfo> {
        return runSuspendCatching(dispatcher) {
            objectCache.getOrElse(ignoreCache) {
                httpClient.get("/api/display/brightness").body<DisplayBrightnessInfo>()
            }
        }
    }

    private fun BsbBrightness.asDisplayBrightnessInfo(): DisplayBrightnessInfo {
        return when (this) {
            BsbBrightness.Auto -> DisplayBrightnessInfo("auto")
            is BsbBrightness.Number -> DisplayBrightnessInfo("${this.value}")
        }
    }

    override suspend fun setDisplayBrightness(
        value: BsbBrightness,
    ): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/display/brightness") {
                parameter("value", value.asDisplayBrightnessInfo().value)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun getAudioVolume(ignoreCache: Boolean): Result<AudioVolumeInfo> {
        return runSuspendCatching(dispatcher) {
            objectCache.getOrElse(ignoreCache) {
                httpClient.get("/api/audio/volume").body<AudioVolumeInfo>()
            }
        }
    }

    override suspend fun setAudioVolume(volume: Int): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/audio/volume") {
                parameter("volume", volume)
            }.body<SuccessResponse>()
        }
    }
}
