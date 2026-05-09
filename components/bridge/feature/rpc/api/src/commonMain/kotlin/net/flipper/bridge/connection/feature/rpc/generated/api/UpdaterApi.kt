package net.flipper.bridge.connection.feature.rpc.generated.api

import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.feature.rpc.generated.model.AutoupdateSettings
import net.flipper.bridge.connection.feature.rpc.generated.model.GetUpdateChangelog200Response
import net.flipper.bridge.connection.feature.rpc.generated.model.SuccessResponse
import net.flipper.bridge.connection.feature.rpc.generated.model.UpdateStatus

interface UpdaterApi {

    /**
     * Abort ongoing firmware download
     */
    suspend fun abortFirmwareDownload(): kotlin.Result<SuccessResponse>

    /**
     * Start firmware update check
     */
    suspend fun checkFirmwareUpdate(): kotlin.Result<SuccessResponse>

    /**
     * Get autoupdate settings
     */
    suspend fun getAutoupdateSettings(): kotlin.Result<AutoupdateSettings>

    /**
     * Get firmware update status
     */
    suspend fun getFirmwareUpdateStatus(): kotlin.Result<UpdateStatus>

    /**
     * Get update changelog
     */
    suspend fun getUpdateChangelog(version: kotlin.String): kotlin.Result<GetUpdateChangelog200Response>

    /**
     * Install firmware update
     */
    suspend fun installFirmwareUpdate(version: kotlin.String): kotlin.Result<SuccessResponse>

    /**
     * Set autoupdate settings
     */
    suspend fun setAutoupdateSettings(autoupdateSettings: AutoupdateSettings): kotlin.Result<SuccessResponse>

    /**
     * Update firmware
     */
    suspend fun updateFirmware(
        bytesFlow: Flow<ByteArray>,
        totalBytes: Long,
        onTransferred: (Long) -> Unit
    ): Result<Unit>
}
