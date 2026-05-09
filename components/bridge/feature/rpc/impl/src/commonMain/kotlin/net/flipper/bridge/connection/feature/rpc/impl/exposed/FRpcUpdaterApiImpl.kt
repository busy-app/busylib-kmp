package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.yield
import net.flipper.bridge.connection.feature.rpc.api.model.ApiResponse
import net.flipper.bridge.connection.feature.rpc.api.model.ErrorResponse
import net.flipper.bridge.connection.feature.rpc.generated.api.UpdaterApi
import net.flipper.bridge.connection.feature.rpc.generated.model.AutoupdateSettings
import net.flipper.bridge.connection.feature.rpc.generated.model.GetUpdateChangelog200Response
import net.flipper.bridge.connection.feature.rpc.generated.model.SuccessResponse
import net.flipper.bridge.connection.feature.rpc.generated.model.UpdateStatus
import net.flipper.core.busylib.ktx.common.chunked
import net.flipper.core.busylib.ktx.common.runSuspendCatching

class FRpcUpdaterApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher,
) : UpdaterApi {
    override suspend fun checkFirmwareUpdate(): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            val httpResponse = httpClient.post("/api/update/check")
            val response = httpResponse.body<ApiResponse>()
            when (response) {
                is ErrorResponse -> {
                    if (httpResponse.status == HttpStatusCode.Conflict) {
                        SuccessResponse(response.error) // Operation already in progress
                    } else {
                        @Suppress("TooGenericExceptionThrown")
                        throw RuntimeException("Received error response: $response")
                    }
                }

                is net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse -> {
                    SuccessResponse(response.result)
                }
            }
        }
    }

    override suspend fun setAutoupdateSettings(autoupdateSettings: AutoupdateSettings): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/update/autoupdate") {
                setBody(autoupdateSettings)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun getAutoupdateSettings(): Result<AutoupdateSettings> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/update/autoupdate").body<AutoupdateSettings>()
        }
    }

    override suspend fun getFirmwareUpdateStatus(): Result<UpdateStatus> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/update/status").body<UpdateStatus>()
        }
    }

    override suspend fun getUpdateChangelog(version: String): Result<GetUpdateChangelog200Response> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/update/changelog") {
                parameter("version", version)
            }.body<GetUpdateChangelog200Response>()
        }
    }

    override suspend fun installFirmwareUpdate(version: String): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/update/install") {
                parameter("version", version)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun abortFirmwareDownload(): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/update/abort_download").body<SuccessResponse>()
        }
    }

    override suspend fun updateFirmware(
        bytesFlow: Flow<ByteArray>,
        totalBytes: Long,
        onTransferred: (Long) -> Unit
    ): Result<Unit> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/update") {
                contentType(ContentType.Application.OctetStream)

                setBody(
                    object : OutgoingContent.WriteChannelContent() {
                        override val contentLength: Long = totalBytes

                        override suspend fun writeTo(channel: ByteWriteChannel) {
                            var transferred = 0L

                            bytesFlow.collect { byteArray ->
                                byteArray.chunked(count = 64).forEach { chunk ->
                                    channel.writeFully(chunk)
                                    channel.flush()
                                    transferred += chunk.size
                                }
                                yield()
                                onTransferred(transferred)
                            }
                        }
                    }
                )
            }
        }.map { }
    }
}
