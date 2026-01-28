package net.flipper.bridge.connection.transport.combined.impl.connections

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info

class WrappedConnectionInternal(
    private val config: FDeviceConnectionConfig<*>,
    parentScope: CoroutineScope,
    private val connectionBuilder: FDeviceConfigToConnection,
    private val dispatcher: CoroutineDispatcher = FlipperDispatchers.default
) : LogTagProvider, FTransportConnectionStatusListener {
    override val TAG = "WrappedConnection"

    private var connectionApi: FConnectedDeviceApi? = null
    val stateFlow: StateFlow<FInternalTransportConnectionStatus>
        field = MutableStateFlow<FInternalTransportConnectionStatus>(
            FInternalTransportConnectionStatus.Connecting
        )

    private val scope = run {
        val job = SupervisorJob(parent = parentScope.coroutineContext[Job]).apply {
            invokeOnCompletion {
                if (it == null) {
                    info { "Wrapped connection $config scope is being cancelled" }
                    stateFlow.value = FInternalTransportConnectionStatus.Disconnected
                } else {
                    error(it) { "Scope for connection $config was cancelled due to an error" }
                }
            }
        }
        parentScope + dispatcher + job
    }

    init {
        scope.launch {
            connectionApi = connectionBuilder.connect(
                scope,
                config,
                this@WrappedConnectionInternal
            ).getOrThrow()
        }
    }

    override suspend fun onStatusUpdate(status: FInternalTransportConnectionStatus) {
        stateFlow.value = status
    }

    suspend fun disconnect() {
        connectionApi?.disconnect()
        scope.cancel()
    }
}