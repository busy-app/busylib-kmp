package com.flipperdevices.bridge.connection.feature.rpc.impl.exposed

import com.flipperdevices.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.model.BleStatusResponse
import com.flipperdevices.bridge.connection.feature.rpc.api.model.BusyBarStatus
import com.flipperdevices.bridge.connection.feature.rpc.api.model.BusyBarStatusPower
import com.flipperdevices.bridge.connection.feature.rpc.api.model.BusyBarStatusSystem
import com.flipperdevices.bridge.connection.feature.rpc.api.model.BusyBarVersion
import com.flipperdevices.bridge.connection.feature.rpc.api.model.ConnectRequestConfig
import com.flipperdevices.bridge.connection.feature.rpc.api.model.NetworkResponse
import com.flipperdevices.bridge.connection.feature.rpc.api.model.SuccessResponse
import com.flipperdevices.bridge.connection.feature.rpc.api.model.WifiStatusResponse
import com.flipperdevices.core.busylib.ktx.common.FlipperDispatchers
import com.flipperdevices.core.busylib.ktx.common.runSuspendCatching
import com.flipperdevices.core.busylib.log.LogTagProvider
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.withContext

@Inject
@Suppress("TooManyFunctions")
class FRpcFeatureApiImpl(
    @Suppress("UnusedPrivateProperty")
    @Assisted private val httpClient: HttpClient
) : FRpcFeatureApi, LogTagProvider {
    override val TAG = "FlipperRequestApi"
    private val dispatcher = FlipperDispatchers.default

    override suspend fun getVersion(): Result<BusyBarVersion> = withContext(dispatcher) {
        return@withContext runCatching {
            httpClient.get("/api/version").body<BusyBarVersion>()
        }
    }

    override suspend fun getStatus(): Result<BusyBarStatus> = withContext(dispatcher) {
        return@withContext runCatching {
            httpClient.get("/api/status").body<BusyBarStatus>()
        }
    }

    override suspend fun getStatusSystem(): Result<BusyBarStatusSystem> = withContext(dispatcher) {
        return@withContext runSuspendCatching {
            httpClient.get("/api/status/system").body<BusyBarStatusSystem>()
        }
    }

    override suspend fun getStatusPower(): Result<BusyBarStatusPower> = withContext(dispatcher) {
        return@withContext runSuspendCatching {
            httpClient.get("/api/status/power").body<BusyBarStatusPower>()
        }
    }

    override suspend fun getWifiNetworks(): Result<NetworkResponse> = withContext(dispatcher) {
        return@withContext runSuspendCatching {
            httpClient.get("/api/wifi/networks").body<NetworkResponse>()
        }
    }

    override suspend fun connectWifi(config: ConnectRequestConfig): Result<SuccessResponse> =
        withContext(dispatcher) {
            return@withContext runSuspendCatching {
                httpClient.post("/api/wifi/connect") {
                    contentType(ContentType.Application.Json)
                    setBody(config)
                }.body<SuccessResponse>()
            }
        }

    override suspend fun disconnectWifi(): Result<SuccessResponse> = withContext(dispatcher) {
        return@withContext runSuspendCatching {
            httpClient.post("/api/wifi/disconnect").body<SuccessResponse>()
        }
    }

    override suspend fun getWifiStatus(): Result<WifiStatusResponse> = withContext(dispatcher) {
        return@withContext runSuspendCatching {
            httpClient.get("/api/wifi/status").body<WifiStatusResponse>()
        }
    }

    override suspend fun getBleStatus(): Result<BleStatusResponse> = withContext(dispatcher) {
        return@withContext runSuspendCatching {
            httpClient.get("/api/ble/status").body<BleStatusResponse>()
        }
    }

    override suspend fun getScreen(display: Int): Result<String> = withContext(dispatcher) {
        return@withContext runSuspendCatching {
            httpClient.get {
                url("/api/screen")
                header(HttpHeaders.Accept, "image/bmp")
                parameter("display", display)
            }.bodyAsText()
        }
    }

    @AssistedFactory
    fun interface InternalFactory {
        operator fun invoke(
            client: HttpClient
        ): FRpcFeatureApiImpl
    }
}
