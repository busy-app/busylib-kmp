package net.flipper.bridge.connection.transport.lan.impl

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.lan.FLanApi
import net.flipper.bridge.connection.transport.lan.FLanDeviceConnectionConfig
import net.flipper.bridge.connection.transport.lan.impl.engine.BUSYBarHttpEngine
import net.flipper.bridge.connection.transport.lan.impl.engine.getPlatformEngineFactory
import net.flipper.bridge.connection.transport.lan.impl.monitor.FLanConnectionMonitor

class FLanApiImpl(
    private val listener: FTransportConnectionStatusListener,
    config: FLanDeviceConnectionConfig,
    private val scope: CoroutineScope
) : FLanApi {
    private val httpEngineOriginal = getPlatformEngineFactory().create()
    private val httpEngine = BUSYBarHttpEngine(httpEngineOriginal, config.host)
    private val connectionMonitor = FLanConnectionMonitor(
        listener,
        config
    )
    override val deviceName = config.host

    init {
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
