package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.api.model.ApiResponse
import net.flipper.bridge.connection.feature.rpc.api.model.BsbRpcError
import net.flipper.bridge.connection.feature.rpc.api.model.ErrorResponse
import net.flipper.bridge.connection.feature.rpc.generated.api.WiFiApi
import net.flipper.bridge.connection.feature.rpc.generated.model.ConnectRequestConfig
import net.flipper.bridge.connection.feature.rpc.generated.model.NetworkResponse
import net.flipper.bridge.connection.feature.rpc.generated.model.StatusResponse
import net.flipper.bridge.connection.feature.rpc.generated.model.SuccessResponse
import net.flipper.core.busylib.ktx.common.runSuspendCatching

class FRpcWifiApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher,
) : WiFiApi {

    override suspend fun getWifiNetworks(): Result<NetworkResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/wifi/networks").body<NetworkResponse>()
        }
    }

    override suspend fun apiWifiConnectPost(config: ConnectRequestConfig): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.post("/api/wifi/connect") {
                setBody(config)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun apiWifiDisconnectPost(): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            val response = httpClient.post("/api/wifi/disconnect").body<ApiResponse>()
            return@runSuspendCatching when (response) {
                is ErrorResponse if response.error == BsbRpcError.ALREADY_CONNECTED.error -> {
                    SuccessResponse(response.error)
                }

                is ErrorResponse -> error(response.error)
                is net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse -> {
                    SuccessResponse(response.result)
                }
            }
        }
    }

    override suspend fun apiWifiStatusGet(): Result<StatusResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/wifi/status").body<StatusResponse>()
        }
    }
}
