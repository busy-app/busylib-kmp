package net.flipper.bridge.connection.transport.combined.impl.connections

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.combined.impl.connections.utils.ChildSupervisorScope
import net.flipper.bridge.connection.transport.combined.impl.connections.utils.WrappedConnectionException
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.ktx.common.launchOnCompletion
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
    private val _stateFlow = MutableStateFlow<FInternalTransportConnectionStatus>(
        FInternalTransportConnectionStatus.Connecting
    )
    val stateFlow: StateFlow<FInternalTransportConnectionStatus> get() = _stateFlow

    private val scope = ChildSupervisorScope(
        parentScope = parentScope,
        dispatcher = dispatcher
    ) {
        if (it == null) {
            info { "Wrapped connection $config scope is being cancelled" }
        } else {
            error(it) { "Scope for connection $config was cancelled due to an error" }
        }
        _stateFlow.value = FInternalTransportConnectionStatus.Disconnected
    }

    init {
        scope.launch {
            connectionApi = connectionBuilder.connect(
                scope,
                config,
                this@WrappedConnectionInternal
            ).getOrElse { throw WrappedConnectionException(it) }
        }
        scope.launchOnCompletion {
            connectionApi?.disconnect()
        }
    }

    override suspend fun onStatusUpdate(status: FInternalTransportConnectionStatus) {
        _stateFlow.value = status
        yield() // Allow collectors to process the state before returning
    }

    suspend fun disconnect() {
        scope.cancel()
    }
}
