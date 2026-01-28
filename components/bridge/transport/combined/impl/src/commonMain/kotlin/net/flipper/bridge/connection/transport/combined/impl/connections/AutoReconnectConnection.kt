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
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.ktx.common.getExponentialDelay
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class AutoReconnectConnection(
    scope: CoroutineScope,
    private val config: FDeviceConnectionConfig<*>,
    private val connectionBuilder: FDeviceConfigToConnection,
    private val dispatcher: CoroutineDispatcher = FlipperDispatchers.default
) : LogTagProvider {
    override val TAG = "AutoReconnectConnection"

    private val connectionJob: Job

    val stateFlow: StateFlow<FInternalTransportConnectionStatus>
        field = MutableStateFlow<FInternalTransportConnectionStatus>(
            FInternalTransportConnectionStatus.Connecting
        )

    init {
        connectionJob = scope.launch {
            var retryCount = 0
            while (isActive) {
                delay(getExponentialDelay(retryCount))
                info { "AutoReconnectConnection: Connecting... $config" }
                val connection = WrappedConnectionInternal(
                    config = config,
                    connectionBuilder = connectionBuilder,
                    parentScope = this,
                    dispatcher = dispatcher
                )
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
                retryCount++
            }
        }
    }

    suspend fun disconnect() {
        connectionJob.cancelAndJoin()
    }
}