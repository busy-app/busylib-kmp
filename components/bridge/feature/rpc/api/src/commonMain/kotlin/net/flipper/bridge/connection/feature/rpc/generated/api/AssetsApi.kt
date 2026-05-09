package net.flipper.bridge.connection.feature.rpc.generated.api

import net.flipper.bridge.connection.feature.rpc.generated.model.DisplayElements
import net.flipper.bridge.connection.feature.rpc.generated.model.PlayAudio
import net.flipper.bridge.connection.feature.rpc.generated.model.SuccessResponse

interface AssetsApi {

    /**
     * Clear display
     */
    suspend fun clearDisplay(applicationName: kotlin.String? = null): kotlin.Result<SuccessResponse>

    /**
     * Delete app assets
     */
    suspend fun deleteAppAssets(applicationName: kotlin.String): kotlin.Result<SuccessResponse>

    /**
     * Draw on display
     */
    suspend fun drawOnDisplay(displayElements: DisplayElements): kotlin.Result<SuccessResponse>

    /**
     * Play audio file
     */
    suspend fun playAudio(playAudio: PlayAudio): kotlin.Result<SuccessResponse>

    /**
     * Stop audio playback
     */
    suspend fun stopAudio(): kotlin.Result<SuccessResponse>

    /**
     * Upload asset file with app ID
     */
    suspend fun uploadAssetWithAppId(
        applicationName: kotlin.String,
        file: kotlin.String,
        body: kotlin.String
    ): kotlin.Result<SuccessResponse>
}
