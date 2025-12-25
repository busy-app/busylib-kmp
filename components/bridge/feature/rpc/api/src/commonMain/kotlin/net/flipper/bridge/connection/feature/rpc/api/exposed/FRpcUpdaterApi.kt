package net.flipper.bridge.connection.feature.rpc.api.exposed

import net.flipper.bridge.connection.feature.rpc.api.model.GetUpdateChangelogResponse
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus

interface FRpcUpdaterApi {
    suspend fun startUpdateCheck(): Result<SuccessResponse>
    suspend fun getUpdateStatus(ignoreCache: Boolean): Result<UpdateStatus>
    suspend fun getUpdateChangelog(version: String): Result<GetUpdateChangelogResponse>
    suspend fun startUpdateInstall(version: String): Result<SuccessResponse>
    suspend fun startUpdateAbortDownload(): Result<SuccessResponse>
}
