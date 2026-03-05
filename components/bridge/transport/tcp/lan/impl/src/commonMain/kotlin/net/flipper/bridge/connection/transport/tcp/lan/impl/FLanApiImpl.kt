package net.flipper.bridge.connection.transport.tcp.lan.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.bridge.connection.transport.tcp.lan.FLanApi
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.BUSYBarHttpEngine
import net.flipper.bridge.connection.transport.tcp.lan.impl.monitor.getConnectionMonitorApi
import net.flipper.core.ktor.getPlatformEngineFactory

class FLanApiImpl(
    private val listener: FTransportConnectionStatusListener,
    private var currentConfig: FLanDeviceConnectionConfig,
    private val scope: CoroutineScope
) : FLanApi {
    private val httpEngineOriginal = getPlatformEngineFactory().create()
    private val httpEngine = BUSYBarHttpEngine(httpEngineOriginal, currentConfig.host)

    private val connectionMonitor = getConnectionMonitorApi(
        listener = listener,
        config = currentConfig,
        scope = scope,
        deviceApi = this
    )

    override val deviceName: String
        get() = currentConfig.name

    private val _capabilities = flowOf(
        listOf(
            FHTTPTransportCapability.BB_WEBSOCKET_SUPPORTED,
            FHTTPTransportCapability.BB_DOWNLOAD_UPDATE_SUPPORTED
        )
    ).shareIn(scope, SharingStarted.WhileSubscribed(), 1)

    override fun getCapabilities(): Flow<List<FHTTPTransportCapability>> {
        return _capabilities
    }

    suspend fun startMonitoring() {
        connectionMonitor.startMonitoring()
    }

    override suspend fun tryUpdateConnectionConfig(config: FDeviceConnectionConfig<*>): Result<Unit> {
        if (config !is FLanDeviceConnectionConfig) {
            return Result.failure(IllegalArgumentException("Config $config has different type"))
        }
        if (currentConfig == config) {
            return Result.success(Unit)
        }
        if (currentConfig.copy(name = config.name) == config) {
            currentConfig = config
            return Result.success(Unit)
        }
        return Result.failure(IllegalArgumentException("Config $config has different non-name fields"))
    }

    override suspend fun disconnect() {
        connectionMonitor.stopMonitoring()
        httpEngine.close()
        httpEngineOriginal.close()
        listener.onStatusUpdate(FInternalTransportConnectionStatus.Disconnected)
    }

    override fun getDeviceHttpEngine() = httpEngine
}
