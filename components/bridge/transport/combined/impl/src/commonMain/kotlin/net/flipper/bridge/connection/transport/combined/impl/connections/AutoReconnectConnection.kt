package net.flipper.bridge.connection.transport.combined.impl.connections

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.ktx.common.getExponentialDelay
import net.flipper.core.busylib.ktx.common.withLockResult
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class AutoReconnectConnection(
    scope: CoroutineScope,
    initialConfig: FDeviceConnectionConfig<*>,
    private val connectionBuilder: FDeviceConfigToConnection,
    private val dispatcher: CoroutineDispatcher = FlipperDispatchers.default
) : LogTagProvider {
    override val TAG = "AutoReconnectConnection"

    var config: FDeviceConnectionConfig<*> = initialConfig
        private set

    private val updateMutex = Mutex()
    private val connectionJob: Job

    private val stateFlowWithDisconnected = MutableStateFlow<FInternalTransportConnectionStatus>(
        getConnectingStatus()
    )

    val stateFlow = stateFlowWithDisconnected.map { it.toNotDisconnectedStatus() }

    init {
        connectionJob = scope.launch {
            var retryCount = 0
            while (isActive) {
                // Mutex prevents creating a new WrappedConnectionInternal
                // while tryUpdateConnectionConfig is in progress
                val connection = withLockResult(updateMutex) {
                    val currentConfig = config
                    info { "AutoReconnectConnection: Connecting... $currentConfig" }
                    // One WrappedConnectionInternal, one device api always
                    WrappedConnectionInternal(
                        config = currentConfig,
                        connectionBuilder = connectionBuilder,
                        parentScope = this@launch,
                        dispatcher = dispatcher
                    )
                }
                info { "Created connection $connection" }

                val disconnectedStatus = connection.stateFlow
                    .onEach { connectionStatus ->
                        info { "Got connection status $connectionStatus" }
                        stateFlowWithDisconnected.emit(connectionStatus)
                        if (connectionStatus is FInternalTransportConnectionStatus.Connected) {
                            retryCount = 0
                        }
                    }
                    .filterIsInstance<FInternalTransportConnectionStatus.Disconnected>()
                    .first()
                info { "Got disconnected event ${disconnectedStatus.reason}" }
                connection.disconnect()

                if (!disconnectedStatus.reason.isRecoverable) {
                    return@launch
                }

                delay(getExponentialDelay(retryCount))
                retryCount++
            }
        }
    }

    suspend fun tryUpdateConnectionConfig(
        newConfig: FDeviceConnectionConfig<*>
    ): Result<Unit> = withLockResult(updateMutex) {
        val currentState = stateFlow.first()
        if (currentState !is FInternalTransportConnectionStatus.Connected) {
            return@withLockResult if (config == newConfig) {
                Result.success(Unit)
            } else {
                Result.failure(
                    IllegalStateException("Cannot tryUpdateConnectionConfig: not connected")
                )
            }
        }

        return@withLockResult currentState.deviceApi
            .tryUpdateConnectionConfig(newConfig)
            .onSuccess { config = newConfig }
    }

    suspend fun disconnect() {
        connectionJob.cancelAndJoin()
    }

    private fun FInternalTransportConnectionStatus.toNotDisconnectedStatus(): FInternalTransportConnectionStatus {
        return when (this) {
            is FInternalTransportConnectionStatus.Disconnected ->
                if (reason.isRecoverable) {
                    getConnectingStatus()
                } else {
                    this
                }

            else -> this
        }
    }

    private fun getConnectingStatus(): FInternalTransportConnectionStatus.Connecting {
        return FInternalTransportConnectionStatus.Connecting(config.getTransportTypes())
    }
}
