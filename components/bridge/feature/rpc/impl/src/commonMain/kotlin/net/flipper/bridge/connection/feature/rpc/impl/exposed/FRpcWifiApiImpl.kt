package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcWifiApi
import net.flipper.bridge.connection.feature.rpc.api.model.ApiResponse
import net.flipper.bridge.connection.feature.rpc.api.model.BsbRpcError
import net.flipper.bridge.connection.feature.rpc.api.model.ConnectRequestConfig
import net.flipper.bridge.connection.feature.rpc.api.model.ErrorResponse
import net.flipper.bridge.connection.feature.rpc.api.model.NetworkResponse
import net.flipper.bridge.connection.feature.rpc.api.model.StatusResponse
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.core.busylib.ktx.common.cache.ObjectCache
import net.flipper.core.busylib.ktx.common.cache.getOrElse
import net.flipper.core.busylib.ktx.common.runSuspendCatching

class FRpcWifiApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher,
    private val objectCache: ObjectCache
) : FRpcWifiApi {

    override suspend fun getWifiNetworks(): Result<NetworkResponse> {
        return runSuspendCatching(dispatcher) {
            httpClient.get("/api/wifi/networks").body<NetworkResponse>()
        }
    }

    override suspend fun connectWifi(config: ConnectRequestConfig): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            val response = httpClient.post("/api/wifi/connect") {
                setBody(config)
            }.body<ApiResponse>()

            return@runSuspendCatching when (response) {
                is ErrorResponse if response.error == BsbRpcError.ALREADY_CONNECTED.error -> {
                    SuccessResponse(response.error)
                }

                is ErrorResponse -> error(response.error)
                is SuccessResponse -> response
            }
        }
    }

    override suspend fun disconnectWifi(): Result<SuccessResponse> {
        return runSuspendCatching(dispatcher) {
            val response = httpClient.post("/api/wifi/disconnect").body<ApiResponse>()
            return@runSuspendCatching when (response) {
                is ErrorResponse if response.error == BsbRpcError.ALREADY_CONNECTED.error -> {
                    SuccessResponse(response.error)
                }

                is ErrorResponse -> error(response.error)
                is SuccessResponse -> response
            }
        }
    }

    override suspend fun getWifiStatus(ignoreCache: Boolean): Result<StatusResponse> {
        return runSuspendCatching(dispatcher) {
            objectCache.getOrElse(ignoreCache) {
                httpClient.get("/api/wifi/status").body<StatusResponse>()
            }
        }
    }
}
