package net.flipper.bridge.connection.feature.rpc.generated.api

import net.flipper.bridge.connection.feature.rpc.generated.model.StorageList
import net.flipper.bridge.connection.feature.rpc.generated.model.StorageStatus
import net.flipper.bridge.connection.feature.rpc.generated.model.SuccessResponse
import kotlin.String

interface StorageApi {

    /**
     * Create a directory on internal storage
     */
    suspend fun createStorageDir(path: kotlin.String): kotlin.Result<SuccessResponse>

    /**
     * Show storage usage
     */
    suspend fun getStorageStatus(): kotlin.Result<StorageStatus>

    /**
     * List files on internal storage
     */
    suspend fun listStorageFiles(path: kotlin.String): kotlin.Result<StorageList>

    /**
     * Download file from internal storage
     */
    suspend fun readStorageFile(path: kotlin.String): kotlin.Result<kotlin.String>

    /**
     * Remove a file on internal storage
     */
    suspend fun removeStorageFile(path: kotlin.String): kotlin.Result<SuccessResponse>

    /**
     * Rename/move a file
     */
    suspend fun renameStorageFile(path: kotlin.String, newPath: kotlin.String): kotlin.Result<SuccessResponse>

    /**
     * Upload file to internal storage
     */
    suspend fun writeStorageFile(path: kotlin.String, body: kotlin.String): kotlin.Result<SuccessResponse>
}
