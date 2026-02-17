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
import net.flipper.core.busylib.ktx.common.withLock
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
                updateConnectionConfigUnsafe(config)
            }
        }
    }

    private suspend fun updateConnectionConfigUnsafe(config: FCombinedConnectionConfig) {
        val oldConnections = connections.value
        val matchedOldIndices = mutableSetOf<Int>()
        val newConnectionsList = mutableListOf<AutoReconnectConnection>()

        for (newChildConfig in config.connectionConfigs) {
            var matched = false
            // 1. Exact config match — reuse without calling tryUpdateConnectionConfig
            for ((idx, oldConn) in oldConnections.withIndex()) {
                if (idx in matchedOldIndices) continue
                if (oldConn.config == newChildConfig) {
                    newConnectionsList.add(oldConn)
                    matchedOldIndices.add(idx)
                    matched = true
                    info { "Found exact match for $newChildConfig" }
                    break
                }
            }
            if (matched) continue

            // 2. Try tryUpdateConnectionConfig on unmatched existing connections
            for ((idx, oldConn) in oldConnections.withIndex()) {
                val result = oldConn.tryUpdateConnectionConfig(newChildConfig)
                if (result.isSuccess) {
                    newConnectionsList.add(oldConn)
                    matchedOldIndices.add(idx)
                    matched = true
                    info { "Successfully updated $newChildConfig with ${oldConn.config}" }
                    break
                }
            }
            if (matched) continue

            info { "Create new connection for $newChildConfig" }
            // 3. Create new AutoReconnectConnection
            newConnectionsList.add(
                AutoReconnectConnection(
                    scope = scope,
                    initialConfig = newChildConfig,
                    connectionBuilder = connectionBuilder
                )
            )
        }

        // Update connections flow first so consumers see new list immediately
        connections.value = newConnectionsList
        currentConfig = config

        // Disconnect removed connections
        for ((idx, oldConn) in oldConnections.withIndex()) {
            if (idx !in matchedOldIndices) {
                runSuspendCatching { oldConn.disconnect() }
            }
        }
    }

    private val httpEngine = FCombinedHttpEngine(scope, connections)
    private val metaInfoApi = CombinedMetaInfoApiImpl(connections)

    override fun getDeviceHttpEngine(): HttpClientEngine {
        return httpEngine
    }

    override fun get(key: TransportMetaInfoKey): Flow<Result<Flow<ByteArray?>>> {
        return metaInfoApi.get(key)
    }

    override suspend fun disconnect() {
        connections.value.forEach {
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
