package net.flipper.bridge.connection.transport.combined.impl

import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionApi
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionConfig
import net.flipper.bridge.connection.transport.combined.impl.connections.AutoReconnectConnection
import net.flipper.bridge.connection.transport.combined.impl.connections.ConnectionSnapshot
import net.flipper.bridge.connection.transport.combined.impl.connections.SharedConnectionPool
import net.flipper.bridge.connection.transport.combined.impl.metakey.CombinedMetaInfoApiImpl
import net.flipper.bridge.connection.transport.combined.impl.streaming.FCombinedStreamingApiImpl
import net.flipper.bridge.connection.transport.combined.impl.utils.UpdateConfigDelegate
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoData
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi
import net.flipper.bridge.connection.transport.common.api.serial.StatusStreamingEvent
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.ktx.common.withLockResult
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

@OptIn(ExperimentalCoroutinesApi::class)
class FCombinedConnectionApiImpl(
    private var currentConfig: FCombinedConnectionConfig,
    initialConnections: List<AutoReconnectConnection>,
    private val listener: FTransportConnectionStatusListener,
    private val connectionBuilder: FDeviceConfigToConnection,
    private val scope: CoroutineScope
) : FCombinedConnectionApi, LogTagProvider, FStatusStreamingApi {
    override val TAG = "FCombinedConnectionApi"

    // Visible for testing
    val connections: StateFlow<List<AutoReconnectConnection>>
        field = MutableStateFlow(initialConnections)
    private val connectionPool = SharedConnectionPool(
        scope = scope,
        connectionsFlow = connections
    )

    private val updateMutex = Mutex()
    private fun getCurrentConnectionSnapshotFlow(): Flow<ConnectionSnapshot?> {
        return connectionPool
            .get()
            .map { connectionsList ->
                if (connectionsList.isEmpty()) {
                    null
                } else {
                    connectionsList
                        .maxBy { connectionSnapshot -> getPriority(connectionSnapshot.status) }
                }
            }
            .distinctUntilChanged()
    }

    private fun startCollectTransportStatusUpdateJob(): Job {
        return getCurrentConnectionSnapshotFlow()
            .onEach { connectionSnapshot ->
                val transportConnectionStatus = connectionSnapshot
                    ?.status
                    ?: FInternalTransportConnectionStatus.Disconnected

                listener.onStatusUpdate(transportConnectionStatus)
            }
            .launchIn(scope)
    }

    init {
        startCollectTransportStatusUpdateJob()
    }

    override val deviceName: String
        get() = currentConfig.name

    override suspend fun tryUpdateConnectionConfig(
        config: FDeviceConnectionConfig<*>
    ): Result<Unit> {
        if (config !is FCombinedConnectionConfig) {
            info { "#tryUpdateConnectionConfig configs is not FCombinedConnectionConfig" }
            return Result.failure(IllegalArgumentException("Config $config has different type"))
        }
        if (currentConfig == config) {
            info { "#tryUpdateConnectionConfig configs are identical" }
            return Result.success(Unit)
        }
        info { "Start update child connection configs: $config" }

        return withLockResult(updateMutex) {
            if (currentConfig == config) {
                return@withLockResult Result.success(Unit)
            }

            runSuspendCatching {
                val newConnections = UpdateConfigDelegate.updateConnectionConfigUnsafe(
                    oldConnections = connections.value,
                    config = config,
                    factory = { AutoReconnectConnection(scope, it, connectionBuilder) }
                )
                connections.value = newConnections
                currentConfig = config
            }
        }
    }

    private val _capabilities = connectionPool.get().map { currentConnections ->
        currentConnections
            .flatMap { connectionSnapshot -> connectionSnapshot.capabilities.orEmpty() }
            .distinct()
    }

    override fun getCapabilities(): Flow<List<FHTTPTransportCapability>> {
        return _capabilities
    }

    private val httpEngine = FCombinedHttpEngine(connectionPool)
    private val metaInfoApi = CombinedMetaInfoApiImpl(connectionPool)

    override fun getDeviceHttpEngine(): HttpClientEngine {
        return httpEngine
    }

    override fun get(key: TransportMetaInfoKey): Flow<Result<Flow<TransportMetaInfoData?>>> {
        return metaInfoApi.get(key)
    }

    override suspend fun disconnect() {
        connections.value.forEach {
            runSuspendCatching { it.disconnect() }
        }
    }

    private val streamingApi = FCombinedStreamingApiImpl(connectionPool)

    override fun getEvents(): Flow<StatusStreamingEvent> {
        return streamingApi.getEvents()
    }
}

@Suppress("MagicNumber")
private fun getPriority(status: FInternalTransportConnectionStatus): Int {
    return when (status) {
        FInternalTransportConnectionStatus.Disconnected -> 0
        is FInternalTransportConnectionStatus.Connecting -> 1
        FInternalTransportConnectionStatus.Disconnecting -> 2
        FInternalTransportConnectionStatus.Pairing -> 3
        is FInternalTransportConnectionStatus.Connected -> 4
    }
}
