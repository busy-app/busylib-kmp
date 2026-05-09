package net.flipper.bridge.connection.feature.rpc.generated.api

import net.flipper.bridge.connection.feature.rpc.generated.model.AudioVolumeInfo
import net.flipper.bridge.connection.feature.rpc.generated.model.DisplayBrightnessInfo
import net.flipper.bridge.connection.feature.rpc.generated.model.HttpAccessInfo
import net.flipper.bridge.connection.feature.rpc.generated.model.NameInfo
import net.flipper.bridge.connection.feature.rpc.generated.model.SuccessResponse

interface SettingsApi {

    /**
     * Get current device name
     */
    suspend fun apiNameGet(): kotlin.Result<NameInfo>

    /**
     * Set new device name
     */
    suspend fun apiNamePost(nameInfo: NameInfo): kotlin.Result<SuccessResponse>

    /**
     * Get audio volume
     */
    suspend fun getAudioVolume(): kotlin.Result<AudioVolumeInfo>

    /**
     * Get display brightness
     */
    suspend fun getDisplayBrightness(): kotlin.Result<DisplayBrightnessInfo>

    /**
     * Get HTTP API access over Wi-Fi configuration
     */
    suspend fun getHttpAccess(): kotlin.Result<HttpAccessInfo>

    /**
     * Set audio volume
     */
    suspend fun setAudioVolume(volume: kotlin.Double, silent: kotlin.Int? = null): kotlin.Result<SuccessResponse>

    /**
     * Set display brightness
     */
    suspend fun setDisplayBrightness(`value`: kotlin.String): kotlin.Result<SuccessResponse>

    /**
     * Set HTTP API access over Wi-Fi configuration
     */
    suspend fun setHttpAccess(mode: kotlin.String, key: kotlin.String? = null): kotlin.Result<SuccessResponse>
}
