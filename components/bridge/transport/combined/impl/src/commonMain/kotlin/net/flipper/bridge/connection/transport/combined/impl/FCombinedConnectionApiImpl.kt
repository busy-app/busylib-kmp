package net.flipper.bridge.connection.transport.combined.impl

import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.sync.Mutex
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionApi
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionConfig
import net.flipper.bridge.connection.transport.combined.impl.connections.AutoReconnectConnection
import net.flipper.bridge.connection.transport.combined.impl.metakey.CombinedMetaInfoApiImpl
import net.flipper.bridge.connection.transport.combined.impl.utils.UpdateConfigDelegate
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoData
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.core.busylib.ktx.common.combine
import net.flipper.core.busylib.ktx.common.flatten
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.ktx.common.tryCast
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
) : FCombinedConnectionApi, LogTagProvider {
    override val TAG = "FCombinedConnectionApi"

    // Visible for testing
    val connections: StateFlow<List<AutoReconnectConnection>>
        field = MutableStateFlow(initialConnections)

    private val updateMutex = Mutex()

    init {
        connections.flatMapLatest { connectionsList ->
            if (connectionsList.isEmpty()) {
                flowOf(FInternalTransportConnectionStatus.Disconnected)
            } else {
                combine(connectionsList.map { it.stateFlow }) { states ->
                    states.maxBy { getPriority(it) }
                }
            }
        }.distinctUntilChanged()
            .onEach {
                if (it is FInternalTransportConnectionStatus.Connected) {
                    listener.onStatusUpdate(
                        FInternalTransportConnectionStatus.Connected(
                            scope = scope,
                            deviceApi = this
                        )
                    )
                } else {
                    listener.onStatusUpdate(it)
                }
            }.launchIn(scope)
    }

    override val deviceName: String
        get() = currentConfig.name

    override suspend fun tryUpdateConnectionConfig(
        config: FDeviceConnectionConfig<*>
    ): Result<Unit> {
        if (config !is FCombinedConnectionConfig) {
            return Result.failure(
                IllegalArgumentException("Config $config has different type")
            )
        }
        if (currentConfig == config) {
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

    private val _capabilities = connections.flatMapLatest { connectionsList ->
        connectionsList
            .map(AutoReconnectConnection::stateFlow)
            .combine()
            .flatMapLatest { connectionStatuses ->
                connectionStatuses
                    .asSequence()
                    .filterIsInstance<FInternalTransportConnectionStatus.Connected>()
                    .map { status -> status.deviceApi }
                    .mapNotNull { deviceApi -> deviceApi.tryCast<FHTTPDeviceApi>() }
                    .map(FHTTPDeviceApi::getCapabilities)
                    .toList()
                    .combine()
                    .flatten()
            }
    }.shareIn(scope, SharingStarted.WhileSubscribed(), 1)

    override fun getCapabilities(): Flow<List<FHTTPTransportCapability>> {
        return _capabilities
    }

    private val httpEngine = FCombinedHttpEngine(scope, connections)
    private val metaInfoApi = CombinedMetaInfoApiImpl(connections)

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
}

@Suppress("MagicNumber")
private fun getPriority(status: FInternalTransportConnectionStatus): Int {
    return when (status) {
        FInternalTransportConnectionStatus.Disconnected -> 0
        FInternalTransportConnectionStatus.Connecting -> 1
        FInternalTransportConnectionStatus.Disconnecting -> 2
        FInternalTransportConnectionStatus.Pairing -> 3
        is FInternalTransportConnectionStatus.Connected -> 4
    }
}
