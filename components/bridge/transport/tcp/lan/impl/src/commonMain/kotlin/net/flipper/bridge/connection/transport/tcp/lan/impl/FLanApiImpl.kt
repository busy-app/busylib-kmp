package net.flipper.bridge.connection.transport.tcp.lan.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalDisconnectedReason
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.bridge.connection.transport.tcp.lan.FLanApi
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.BUSYBarHttpEngine
import net.flipper.bridge.connection.transport.tcp.lan.impl.streaming.FLanStreamingApiImpl
import net.flipper.bridge.lanmonitor.api.BB_HOST
import net.flipper.bridge.lanmonitor.api.LanMonitorApi
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.cancelPrevious
import net.flipper.core.ktor.getHttpClient
import net.flipper.core.ktor.getPlatformEngineFactory

class FLanApiImpl(
    private val listener: FTransportConnectionStatusListener,
    private var currentConfig: FLanDeviceConnectionConfig,
    private val scope: CoroutineScope,
    private val lanMonitorApi: LanMonitorApi
) : FLanApi {
    private val httpEngineOriginal = getPlatformEngineFactory().create()
    private val httpEngine = BUSYBarHttpEngine(httpEngineOriginal, BB_HOST)
    private val httpClient = getHttpClient(httpEngine)
    private val lanStreamingApi = FLanStreamingApiImpl(httpClient, scope)
    private val lanMonitorApiScope = scope.asSingleJobScope()

    override val deviceName: String
        get() = currentConfig.name

    private val _capabilities = flowOf(
        listOf(
            FHTTPTransportCapability.BB_WEBSOCKET_SUPPORTED,
            FHTTPTransportCapability.BB_DOWNLOAD_UPDATE_SUPPORTED,
            FHTTPTransportCapability.BB_LOCAL_CONNECTION,
        )
    ).shareIn(scope, SharingStarted.WhileSubscribed(), 1)

    override fun getCapabilities(): Flow<List<FHTTPTransportCapability>> {
        return _capabilities
    }

    fun startMonitoring() {
        lanMonitorApiScope.launch(SingleJobMode.CANCEL_PREVIOUS) {
            lanMonitorApi
                .getConnectedDeviceFlow()
                .mapLatest { metaInfo ->
                    if (metaInfo == null
                        || currentConfig.hardwareId == null
                        || metaInfo.hardwareId != currentConfig.hardwareId
                    ) {
                        FInternalTransportConnectionStatus.Connecting(
                            FInternalTransportConnectionType.LAN
                        )
                    } else {
                        FInternalTransportConnectionStatus.Connected(
                            scope = scope,
                            deviceApi = this@FLanApiImpl,
                            connectionType = FInternalTransportConnectionType.LAN
                        )
                    }
                }.collectLatest {
                    listener.onStatusUpdate(it)
                }
        }
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

    override fun getEvents() = lanStreamingApi.getEvents()

    override suspend fun disconnect() {
        lanMonitorApiScope.cancelPrevious()
        httpEngine.close()
        httpEngineOriginal.close()
        httpClient.close()
        listener.onStatusUpdate(
            FInternalTransportConnectionStatus.Disconnected(
                FInternalDisconnectedReason.OTHER
            )
        )
    }

    override fun getDeviceHttpEngine() = httpEngine
}
