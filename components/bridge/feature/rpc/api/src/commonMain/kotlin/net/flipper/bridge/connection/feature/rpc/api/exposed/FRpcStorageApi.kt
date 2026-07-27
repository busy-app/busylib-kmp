package net.flipper.bridge.connection.feature.rpc.api.exposed

import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse

interface FRpcStorageApi {
    suspend fun writeFile(
        path: String,
        content: ByteArray
    ): Result<SuccessResponse>

    suspend fun createDirectory(path: String): Result<SuccessResponse>

    suspend fun removeFile(path: String): Result<SuccessResponse>
}
