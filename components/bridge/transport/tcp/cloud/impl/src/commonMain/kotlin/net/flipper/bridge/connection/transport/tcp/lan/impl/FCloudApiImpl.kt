package net.flipper.bridge.connection.transport.tcp.lan.impl

import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudApi
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudDeviceConnectionConfig
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.BUSYCloudHttpEngine
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.BUSYCloudHttpEngineFactory
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.token.ProxyTokenProviderFactory
import net.flipper.bridge.connection.transport.tcp.lan.impl.monitor.CloudDeviceMonitor
import net.flipper.core.ktor.getPlatformEngineFactory

class FCloudApiImpl(
    private val listener: FTransportConnectionStatusListener,
    private var currentConfig: FCloudDeviceConnectionConfig,
    cloudDeviceMonitorFactory: CloudDeviceMonitor.Factory,
    tokenProviderFactory: ProxyTokenProviderFactory,
    cloudEngineFactory: BUSYCloudHttpEngineFactory
) : FCloudApi {
    private val httpEngineOriginal = getPlatformEngineFactory().create()
    private val httpEngine = cloudEngineFactory(
        httpEngineOriginal,
        tokenProviderFactory(
            httpEngineOriginal,
            currentConfig.deviceId
        )
    )
    private val cloudDeviceMonitor = cloudDeviceMonitorFactory.create(
        deviceApi = this,
        deviceId = currentConfig.deviceId
    )

    init {
        cloudDeviceMonitor.subscribe(listener)
    }

    override val deviceName: String
        get() = currentConfig.name

    override suspend fun tryUpdateConnectionConfig(config: FDeviceConnectionConfig<*>): Result<Unit> {
        if (config !is FCloudDeviceConnectionConfig) {
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
        httpEngine.close()
        httpEngineOriginal.close()
        listener.onStatusUpdate(FInternalTransportConnectionStatus.Disconnected)
    }

    override fun getDeviceHttpEngine() = httpEngine
}
