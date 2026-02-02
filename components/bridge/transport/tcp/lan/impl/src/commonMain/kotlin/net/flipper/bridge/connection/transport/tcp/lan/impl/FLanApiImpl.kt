package net.flipper.bridge.connection.transport.tcp.lan.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.bridge.connection.transport.tcp.lan.FLanApi
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.BUSYBarHttpEngine
import net.flipper.bridge.connection.transport.tcp.lan.impl.monitor.FLanConnectionMonitorApi
import net.flipper.core.ktor.getPlatformEngineFactory

class FLanApiImpl(
    private val listener: FTransportConnectionStatusListener,
    connectionMonitor: FLanConnectionMonitorApi.Factory,
    config: FLanDeviceConnectionConfig,
    private val scope: CoroutineScope
) : FLanApi {
    private val httpEngineOriginal = getPlatformEngineFactory().create()
    private val httpEngine = BUSYBarHttpEngine(httpEngineOriginal, config.host)

    private val connectionMonitor = connectionMonitor.invoke(
        listener,
        config
    )

    override val deviceName = config.host

    override fun getCapabilities(): MutableStateFlow<List<FHTTPTransportCapability>> {
        return MutableStateFlow(
            listOf(FHTTPTransportCapability.BB_WEBSOCKET_SUPPORTED)
        )
    }

    suspend fun startMonitoring() {
        connectionMonitor.startMonitoring(
            scope = scope,
            deviceApi = this
        )
    }

    override suspend fun disconnect() {
        connectionMonitor.stopMonitoring()
        httpEngine.close()
        httpEngineOriginal.close()
        listener.onStatusUpdate(FInternalTransportConnectionStatus.Disconnected)
    }

    override fun getDeviceHttpEngine() = httpEngine
}
