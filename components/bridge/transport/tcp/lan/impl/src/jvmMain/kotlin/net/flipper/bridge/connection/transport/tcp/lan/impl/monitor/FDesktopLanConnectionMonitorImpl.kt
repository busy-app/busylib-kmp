package net.flipper.bridge.connection.transport.tcp.lan.impl.monitor

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.common.engine.getPlatformEngineFactory
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import kotlin.time.Duration.Companion.seconds

private val MONITORING_INTERVAL = 1.seconds
private val HTTP_TIMEOUT = 3.seconds

@Inject
class FLanConnectionMonitorImpl(
    @Assisted private val listener: FTransportConnectionStatusListener,
    @Assisted private val config: FLanDeviceConnectionConfig
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

    override fun startMonitoring(
        scope: CoroutineScope,
        deviceApi: FConnectedDeviceApi
    ) {
        listener.onStatusUpdate(FInternalTransportConnectionStatus.Connecting)

        // Start monitoring coroutine
        monitoringJob = scope.launch {
            info { "Starting connection monitoring for host: ${config.host}" }
            while (isActive) {
                delay(MONITORING_INTERVAL)
                checkHostAvailability(scope, deviceApi)
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

            // Check that response is successful and doesn't return "false"
            val isAvailable = response.status.isSuccess()

            isAvailable
        } catch (_: Exception) {
            false
        }
    }

    override fun stopMonitoring() {
        info { "Stopping connection monitoring for host: ${config.host}" }
        monitoringJob?.cancel()
        monitoringJob = null
        httpClient.close()
    }

    @Inject
    @ContributesBinding(BusyLibGraph::class, FLanConnectionMonitorApi.Factory::class)
    class InternalFactory(
        private val factory: (
            FTransportConnectionStatusListener,
            FLanDeviceConnectionConfig
        ) -> FLanConnectionMonitorImpl
    ) : FLanConnectionMonitorApi.Factory {
        override operator fun invoke(
            listener: FTransportConnectionStatusListener,
            config: FLanDeviceConnectionConfig
        ): FLanConnectionMonitorApi = factory(listener, config)
    }
}
