package net.flipper.bridge.connection.transport.tcp.lan.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalDisconnectionRecovery
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.bridge.connection.transport.common.api.serial.StatusStreamingEvent
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudApi
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudDeviceConnectionConfig
import net.flipper.bridge.connection.transport.tcp.common.monitor.WSEventsDeviceMonitor
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.BUSYCloudHttpEngineFactory
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.token.ProxyTokenProviderFactory
import net.flipper.bridge.connection.transport.tcp.lan.impl.metainfo.FCloudStreamingFactory
import net.flipper.core.ktor.getPlatformEngineFactory

@Suppress("LongParameterList")
class FCloudApiImpl(
    private val listener: FTransportConnectionStatusListener,
    private var currentConfig: FCloudDeviceConnectionConfig,
    scope: CoroutineScope,
    tokenProviderFactory: ProxyTokenProviderFactory,
    cloudEngineFactory: BUSYCloudHttpEngineFactory,
    cloudStreamingFactory: FCloudStreamingFactory
) : FCloudApi {
    private val httpEngineOriginal = getPlatformEngineFactory().create()
    private val httpEngine = cloudEngineFactory(
        httpEngineOriginal,
        tokenProviderFactory(
            currentConfig.deviceId
        )
    )
    private val cloudStreamingApi = cloudStreamingFactory(currentConfig.deviceId)
    private val wsEventsDeviceMonitor = WSEventsDeviceMonitor(
        deviceApi = this,
        config = currentConfig,
        eventSource = cloudStreamingApi,
        scope = scope,
        listener = listener
    )

    init {
        scope.launch { wsEventsDeviceMonitor.startMonitoring() }
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
        listener.onStatusUpdate(
            FInternalTransportConnectionStatus.Disconnected(
                FInternalDisconnectionRecovery.NON_RECOVERABLE
            )
        )
    }

    override fun getDeviceHttpEngine() = httpEngine

    private val _capabilities = flowOf(
        listOf<FHTTPTransportCapability>()
    ).shareIn(scope, SharingStarted.WhileSubscribed(), 1)

    override fun getCapabilities(): Flow<List<FHTTPTransportCapability>> {
        return _capabilities
    }

    override fun getEvents(): Flow<StatusStreamingEvent> {
        return cloudStreamingApi.getEvents()
    }
}
