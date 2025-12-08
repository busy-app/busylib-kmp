package net.flipper.bridge.connection.transport.lan.impl

import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.lan.FLanApi
import net.flipper.bridge.connection.transport.lan.FLanDeviceConnectionConfig
import net.flipper.bridge.connection.transport.lan.impl.engine.BUSYBarHttpEngine
import net.flipper.bridge.connection.transport.lan.impl.engine.getPlatformEngineFactory

class FLanApiImpl(
    private val listener: FTransportConnectionStatusListener,
    private val config: FLanDeviceConnectionConfig
) : FLanApi {
    private val httpEngineOriginal = getPlatformEngineFactory().create()
    private val httpEngine = BUSYBarHttpEngine(httpEngineOriginal, config.host)

    override suspend fun disconnect() {
        httpEngine.close()
        httpEngineOriginal.close()
        listener.onStatusUpdate(FInternalTransportConnectionStatus.Disconnected)
    }

    override fun getDeviceHttpEngine() = httpEngine
}
