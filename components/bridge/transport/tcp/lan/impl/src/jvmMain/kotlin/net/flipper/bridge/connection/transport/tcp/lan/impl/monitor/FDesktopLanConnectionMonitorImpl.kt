package net.flipper.bridge.connection.transport.tcp.lan.impl.monitor

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.common.engine.getPlatformEngineFactory
import net.flipper.bridge.connection.transport.tcp.lan.FLanApi
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import kotlin.time.Duration.Companion.seconds

private val MONITORING_INTERVAL = 1.seconds
private val HTTP_TIMEOUT = 3.seconds

class FDesktopLanConnectionMonitorImpl(
    private val listener: FTransportConnectionStatusListener,
    private val config: FLanDeviceConnectionConfig,
    private val scope: CoroutineScope,
    private val deviceApi: FLanApi
) : FLanConnectionMonitorApi, LogTagProvider {
    override val TAG: String = "FLanConnectionMonitor"

    private var monitoringJob: Job? = null
    private var isConnected: Boolean = false

    private val httpClient: HttpClient by lazy {
        HttpClient(getPlatformEngineFactory()) {
            install(HttpTimeout) {
                requestTimeoutMillis = HTTP_TIMEOUT.inWholeMilliseconds
                connectTimeoutMillis = HTTP_TIMEOUT.inWholeMilliseconds
                socketTimeoutMillis = HTTP_TIMEOUT.inWholeMilliseconds
            }
        }
    }

    override suspend fun startMonitoring() {
        listener.onStatusUpdate(FInternalTransportConnectionStatus.Connecting)

        // Start monitoring coroutine
        monitoringJob = scope.launch {
            info { "Starting connection monitoring for host: ${config.host}" }
            while (isActive) {
                checkHostAvailability(scope, deviceApi)
                delay(MONITORING_INTERVAL)
            }
        }
    }

    private suspend fun checkHostAvailability(
        scope: CoroutineScope,
        deviceApi: FConnectedDeviceApi
    ) {
        val isHostAvailable = isHostAvailable()

        if (isHostAvailable && !isConnected) {
            info { "Host ${config.host} became available" }
            isConnected = true
            listener.onStatusUpdate(
                FInternalTransportConnectionStatus.Connected(
                    scope = scope,
                    deviceApi = deviceApi
                )
            )
        } else if (!isHostAvailable && isConnected) {
            info { "Host ${config.host} became unavailable" }
            isConnected = false
            listener.onStatusUpdate(FInternalTransportConnectionStatus.Connecting)
        }
    }

    private suspend fun isHostAvailable(): Boolean {
        return try {
            val url = "http://${config.host}/api/version"
            val response = httpClient.get(url)

            // Check that response status is successful
            return response.status.isSuccess()
        } catch (_: Exception) {
            false
        }
    }

    override fun stopMonitoring() {
        info { "Stopping connection monitoring for host: ${config.host}" }
        monitoringJob?.cancel()
        monitoringJob = null
        isConnected = false
    }
}

actual fun getConnectionMonitorApi(
    listener: FTransportConnectionStatusListener,
    config: FLanDeviceConnectionConfig,
    scope: CoroutineScope,
    deviceApi: FLanApi
): FLanConnectionMonitorApi {
    return FDesktopLanConnectionMonitorImpl(
        listener,
        config,
        scope,
        deviceApi
    )
}
