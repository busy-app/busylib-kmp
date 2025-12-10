package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
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
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.AudioVolumeInfo
import net.flipper.bridge.connection.feature.rpc.api.model.BleStatusResponse
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatus
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusPower
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusSystem
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarVersion
import net.flipper.bridge.connection.feature.rpc.api.model.ConnectRequestConfig
import net.flipper.bridge.connection.feature.rpc.api.model.DisplayBrightnessInfo
import net.flipper.bridge.connection.feature.rpc.api.model.DrawRequest
import net.flipper.bridge.connection.feature.rpc.api.model.NetworkResponse
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.bridge.connection.feature.rpc.api.model.WifiStatusResponse
import net.flipper.bridge.connection.feature.rpc.impl.exposed.model.DeviceNameResponse
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider

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

    override suspend fun getBrightnessInfo(): Result<DisplayBrightnessInfo> {
        return withContext(dispatcher) {
            return@withContext runSuspendCatching {
                httpClient.get {
                    url("/api/display/brightness")
                }.body()
            }
        }
    }

    override suspend fun getVolumeInfo(): Result<AudioVolumeInfo> {
        return withContext(dispatcher) {
            return@withContext runSuspendCatching {
                httpClient.get {
                    url("/api/audio/volume")
                }.body()
            }
        }
    }

    override suspend fun getDeviceName(): Result<String> = withContext(dispatcher) {
        return@withContext runSuspendCatching {
            httpClient.get {
                url("/api/name")
            }.body<DeviceNameResponse>().name
        }
    }

    override suspend fun uploadAsset(
        appId: String,
        file: String,
        content: ByteArray
    ): Result<SuccessResponse> = withContext(dispatcher) {
        return@withContext runSuspendCatching {
            httpClient.post("/api/assets/upload") {
                parameter("app_id", appId)
                parameter("file", file)
                contentType(ContentType.Application.OctetStream)
                setBody(content)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun displayDraw(
        request: DrawRequest
    ): Result<SuccessResponse> = withContext(dispatcher) {
        return@withContext runSuspendCatching {
            httpClient.post("/api/display/draw") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<SuccessResponse>()
        }
    }

    override suspend fun removeDraw(
        appId: String
    ): Result<SuccessResponse> = withContext(dispatcher) {
        return@withContext runSuspendCatching {
            httpClient.delete("/api/display/draw") {
                parameter("app_id", appId)
            }
            .body<SuccessResponse>()
        }
    }

    @Inject
    class InternalFactory(
        private val factory: (HttpClient) -> FRpcFeatureApiImpl
    ) {
        operator fun invoke(
            client: HttpClient
        ): FRpcFeatureApiImpl = factory(client)
    }
}
