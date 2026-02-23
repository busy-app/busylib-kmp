package net.flipper.bridge.connection.feature.rpc.api.exposed

import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.feature.rpc.api.model.AutoUpdate
import net.flipper.bridge.connection.feature.rpc.api.model.GetUpdateChangelogResponse
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus

interface FRpcUpdaterApi {
    suspend fun startUpdateCheck(): Result<SuccessResponse>
    suspend fun setAutoUpdate(request: AutoUpdate): Result<SuccessResponse>
    suspend fun getAutoUpdate(): Result<AutoUpdate>
    suspend fun getUpdateStatus(ignoreCache: Boolean): Result<UpdateStatus>
    suspend fun getUpdateChangelog(version: String): Result<GetUpdateChangelogResponse>
    suspend fun startUpdateInstall(version: String): Result<SuccessResponse>
    suspend fun startUpdateAbortDownload(): Result<SuccessResponse>
    suspend fun postUpdate(
        bytesFlow: Flow<ByteArray>,
        totalBytes: Long,
        onTransferred: (Long) -> Unit
    ): Result<Unit>
}
