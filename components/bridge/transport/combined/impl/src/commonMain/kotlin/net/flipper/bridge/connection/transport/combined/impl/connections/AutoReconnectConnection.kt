package net.flipper.bridge.connection.transport.combined.impl.connections

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.ktx.common.getExponentialDelay
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class AutoReconnectConnection(
    scope: CoroutineScope,
    var config: FDeviceConnectionConfig<*>, // Visible for testing
    private val connectionBuilder: FDeviceConfigToConnection,
    private val dispatcher: CoroutineDispatcher = FlipperDispatchers.default
) : LogTagProvider {
    override val TAG = "AutoReconnectConnection"

    private val updateMutex = Mutex()
    private val connectionJob: Job

    val stateFlow: StateFlow<FInternalTransportConnectionStatus>
        field = MutableStateFlow<FInternalTransportConnectionStatus>(
            FInternalTransportConnectionStatus.Connecting
        )

    init {
        connectionJob = scope.launch {
            var retryCount = 0
            while (isActive) {
                // Mutex prevents creating a new WrappedConnectionInternal
                // while tryUpdateConnectionConfig is in progress
                val connection = updateMutex.withLock {
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

                connection.stateFlow
                    .onEach {
                        stateFlow.value = it
                        if (it is FInternalTransportConnectionStatus.Connected) {
                            retryCount = 0
                        }
                    }
                    .filter { it == FInternalTransportConnectionStatus.Disconnected }
                    .first()
                connection.disconnect()
                delay(getExponentialDelay(retryCount))
                retryCount++
            }
        }
    }

    suspend fun tryUpdateConnectionConfig(
        newConfig: FDeviceConnectionConfig<*>
    ): Result<Unit> = updateMutex.withLock {
        config = newConfig
        val currentState = stateFlow.value
        if (currentState !is FInternalTransportConnectionStatus.Connected) {
            return@withLock if (config == newConfig) {
                Result.success(Unit)
            } else {
                Result.failure(
                    IllegalStateException("Cannot tryUpdateConnectionConfig: not connected")
                )
            }
        }

        return@withLock currentState.deviceApi.tryUpdateConnectionConfig(newConfig)
    }

    suspend fun disconnect() {
        connectionJob.cancelAndJoin()
    }
}
