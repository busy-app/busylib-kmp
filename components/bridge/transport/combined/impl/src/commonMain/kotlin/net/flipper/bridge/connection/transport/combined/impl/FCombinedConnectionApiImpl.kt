package net.flipper.bridge.connection.transport.combined.impl

import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionApi
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionConfig
import net.flipper.bridge.connection.transport.combined.impl.connections.AutoReconnectConnection
import net.flipper.bridge.connection.transport.combined.impl.metakey.CombinedMetaInfoApiImpl
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.core.busylib.ktx.common.runSuspendCatching

@OptIn(ExperimentalCoroutinesApi::class)
class FCombinedConnectionApiImpl(
    private var currentConfig: FCombinedConnectionConfig,
    initialConnections: List<AutoReconnectConnection>,
    private val listener: FTransportConnectionStatusListener,
    private val connectionBuilder: FDeviceConfigToConnection,
    private val scope: CoroutineScope
) : FCombinedConnectionApi {

    private val _connections = MutableStateFlow(initialConnections)
    val connectionsFlow: StateFlow<List<AutoReconnectConnection>> get() = _connections

    private val updateMutex = Mutex()

    init {
        _connections.flatMapLatest { connectionsList ->
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

        return updateMutex.withLock {
            if (currentConfig == config) {
                return@withLock Result.success(Unit)
            }

            runCatching {
                val oldConnections = _connections.value
                val matchedOldIndices = mutableSetOf<Int>()
                val newConnectionsList = mutableListOf<AutoReconnectConnection>()

                for (newChildConfig in config.connectionConfigs) {
                    var matched = false

                    // 2. Try tryUpdateConnectionConfig on unmatched existing connections
                    for ((idx, oldConn) in oldConnections.withIndex()) {
                        if (idx in matchedOldIndices) continue
                        val result = runCatching {
                            oldConn.tryUpdateConnectionConfig(newChildConfig)
                        }.getOrElse { Result.failure(it) }
                        if (result.isSuccess) {
                            newConnectionsList.add(oldConn)
                            matchedOldIndices.add(idx)
                            matched = true
                            break
                        }
                    }
                    if (matched) continue

                    // 3. Create new AutoReconnectConnection
                    newConnectionsList.add(
                        AutoReconnectConnection(
                            scope = scope,
                            config = newChildConfig,
                            connectionBuilder = connectionBuilder
                        )
                    )
                }

                // Update connections flow first so consumers see new list immediately
                _connections.value = newConnectionsList
                currentConfig = config

                // Disconnect removed connections
                for ((idx, oldConn) in oldConnections.withIndex()) {
                    if (idx !in matchedOldIndices) {
                        runSuspendCatching { oldConn.disconnect() }
                    }
                }
            }
        }
    }

    private val httpEngine = FCombinedHttpEngine(scope, _connections)
    private val metaInfoApi = CombinedMetaInfoApiImpl(_connections)

    override fun getDeviceHttpEngine(): HttpClientEngine {
        return httpEngine
    }

    override fun get(key: TransportMetaInfoKey): Flow<Result<Flow<ByteArray?>>> {
        return metaInfoApi.get(key)
    }

    override suspend fun disconnect() {
        _connections.value.forEach {
            runSuspendCatching { it.disconnect() }
        }
    }
}

private fun getPriority(status: FInternalTransportConnectionStatus): Int {
    return when (status) {
        FInternalTransportConnectionStatus.Disconnected -> 0
        FInternalTransportConnectionStatus.Connecting -> 1
        FInternalTransportConnectionStatus.Disconnecting -> 2
        FInternalTransportConnectionStatus.Pairing -> 3
        is FInternalTransportConnectionStatus.Connected -> 4
    }
}
