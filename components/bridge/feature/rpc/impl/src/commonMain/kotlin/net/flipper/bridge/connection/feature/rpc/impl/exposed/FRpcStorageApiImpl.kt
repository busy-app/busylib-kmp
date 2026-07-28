package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcStorageApi
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.core.busylib.ktx.common.runSuspendCatching

class FRpcStorageApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher
) : FRpcStorageApi {
    override suspend fun writeFile(
        path: String,
        content: ByteArray
    ): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/storage/write") {
                parameter("path", path)
                contentType(ContentType.Application.OctetStream)
                setBody(content)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun createDirectory(path: String): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/storage/mkdir") {
                parameter("path", path)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun removeFile(path: String): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.delete("/api/storage/remove") {
                parameter("path", path)
            }.body<SuccessResponse>()
        }
    }
}
