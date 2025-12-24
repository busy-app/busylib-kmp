package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcWifiApi
import net.flipper.bridge.connection.feature.rpc.api.model.ConnectRequestConfig
import net.flipper.bridge.connection.feature.rpc.api.model.NetworkResponse
import net.flipper.bridge.connection.feature.rpc.api.model.StatusResponse
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.bridge.connection.feature.rpc.impl.util.runSafely
import net.flipper.core.busylib.ktx.common.cache.ObjectCache
import net.flipper.core.busylib.ktx.common.cache.getOrElse

class FRpcWifiApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher,
    private val objectCache: ObjectCache
) : FRpcWifiApi {

    override suspend fun getWifiNetworks(): Result<NetworkResponse> {
        return runSafely(dispatcher) {
            httpClient.get("/api/wifi/networks").body<NetworkResponse>()
        }
    }

    override suspend fun connectWifi(config: ConnectRequestConfig): Result<SuccessResponse> {
        return runSafely(dispatcher) {
            httpClient.post("/api/wifi/connect") {
                contentType(ContentType.Application.Json)
                setBody(config)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun disconnectWifi(): Result<SuccessResponse> {
        return runSafely(dispatcher) {
            httpClient.post("/api/wifi/disconnect").body<SuccessResponse>()
        }
    }

    override suspend fun getWifiStatus(ignoreCache: Boolean): Result<StatusResponse> {
        return runSafely(dispatcher) {
            objectCache.getOrElse(ignoreCache) { httpClient.get("/api/wifi/status").body<StatusResponse>() }
        }
    }
}
