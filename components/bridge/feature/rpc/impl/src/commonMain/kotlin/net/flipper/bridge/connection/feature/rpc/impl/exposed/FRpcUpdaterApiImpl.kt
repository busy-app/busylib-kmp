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
import net.flipper.core.busylib.ktx.common.cache.ObjectCache
import net.flipper.core.busylib.ktx.common.cache.getOrElse
import net.flipper.core.busylib.ktx.common.runSuspendCatching

class FRpcUpdaterApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher,
    private val objectCache: ObjectCache
) : FRpcUpdaterApi {
    override suspend fun startUpdateCheck(): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/update/check").body<SuccessResponse>()
        }
    }

    override suspend fun getUpdateStatus(ignoreCache: Boolean): Result<UpdateStatus> {
        return runSuspendCatching(dispatcher) {
            objectCache.getOrElse(ignoreCache) {
                httpClient.get("/api/update/status").body<UpdateStatus>()
            }
        }
    }

    override suspend fun getUpdateChangelog(version: String): Result<GetUpdateChangelogResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/update/changelog") {
                parameter("version", version)
            }.body<GetUpdateChangelogResponse>()
        }
    }

    override suspend fun startUpdateInstall(version: String): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/update/install") {
                parameter("version", version)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun startUpdateAbortDownload(): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/update/abort_download").body<SuccessResponse>()
        }
    }
}
