package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcUpdaterApi
import net.flipper.bridge.connection.feature.rpc.api.model.GetUpdateChangelogResponse
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import net.flipper.bridge.connection.feature.rpc.impl.util.runSafely

class FRpcUpdaterApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher
) : FRpcUpdaterApi {
    override suspend fun startUpdateCheck(): Result<SuccessResponse> {
        return runSafely(dispatcher) {
            httpClient.post("/api/update/check").body<SuccessResponse>()
        }
    }

    override suspend fun getUpdateStatus(): Result<UpdateStatus> {
        return runSafely(dispatcher) {
            httpClient.get("/api/update/status").body<UpdateStatus>()
        }
    }

    override suspend fun getUpdateChangelog(version: String): Result<GetUpdateChangelogResponse> {
        return runSafely(dispatcher) {
            httpClient.get("/api/update/changelog") {
                parameter("version", version)
            }.body<GetUpdateChangelogResponse>()
        }
    }

    override suspend fun startUpdateInstall(version: String): Result<SuccessResponse> {
        return runSafely(dispatcher) {
            httpClient.post("/api/update/install") {
                parameter("version", version)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun startUpdateAbortDownload(): Result<SuccessResponse> {
        return runSafely(dispatcher) {
            httpClient.post("/api/update/abort_download").body<SuccessResponse>()
        }
    }
}
