package net.flipper.bridge.connection.feature.rpc.api.exposed

import net.flipper.bridge.connection.feature.rpc.api.model.AudioVolumeInfo
import net.flipper.bridge.connection.feature.rpc.api.model.BsbBrightness
import net.flipper.bridge.connection.feature.rpc.api.model.DisplayBrightnessInfo
import net.flipper.bridge.connection.feature.rpc.api.model.NameInfo
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse

interface FRpcSettingsApi {
    suspend fun getName(ignoreCache: Boolean): Result<NameInfo>
    suspend fun setName(body: NameInfo): Result<SuccessResponse>
    suspend fun getDisplayBrightness(ignoreCache: Boolean): Result<DisplayBrightnessInfo>
    suspend fun setDisplayBrightness(
        front: BsbBrightness,
        back: BsbBrightness
    ): Result<SuccessResponse>

    suspend fun getAudioVolume(ignoreCache: Boolean): Result<AudioVolumeInfo>
    suspend fun setAudioVolume(volume: Int): Result<SuccessResponse>
}
