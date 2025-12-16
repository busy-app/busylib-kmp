package net.flipper.bridge.connection.transport.tcp.lan.impl

import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudApi
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudDeviceConnectionConfig
import net.flipper.bridge.connection.transport.tcp.common.engine.getPlatformEngineFactory
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.BUSYCloudHttpEngine

class FCloudApiImpl(
    private val listener: FTransportConnectionStatusListener,
    config: FCloudDeviceConnectionConfig
) : FCloudApi {
    private val httpEngineOriginal = getPlatformEngineFactory().create()
    private val httpEngine = BUSYCloudHttpEngine(
        httpEngineOriginal,
        authToken = config.authToken,
        host = config.host
    )

    override val deviceName = config.name

    override suspend fun disconnect() {
        httpEngine.close()
        httpEngineOriginal.close()
        listener.onStatusUpdate(FInternalTransportConnectionStatus.Disconnected)
    }

    override fun getDeviceHttpEngine() = httpEngine
}
