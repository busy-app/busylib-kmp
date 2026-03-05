package net.flipper.bridge.connection.transport.combined.impl.connections

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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
    val stateFlow: StateFlow<FInternalTransportConnectionStatus>
        field = MutableStateFlow<FInternalTransportConnectionStatus>(
            FInternalTransportConnectionStatus.Connecting
        )

    private val scope = ChildSupervisorScope(
        parentScope = parentScope,
        dispatcher = dispatcher,
        onCompletion = { t ->
            when (t) {
                null -> {
                    info { "Wrapped connection $config scope is being cancelled" }
                }

                is CancellationException -> {
                    info { "Wrapped connection $config scope was cancelled" }
                }

                else -> {
                    error(t) { "Scope for connection $config was cancelled due to an error" }
                }
            }
            stateFlow.update { FInternalTransportConnectionStatus.Disconnected }
        }
    )

    private fun initConnectionApi() {
        scope.launch {
            info { "#init connect connectionApi with config $config" }
            val connectionApiResult = connectionBuilder.connect(
                scope = scope,
                config = config,
                listener = this@WrappedConnectionInternal
            )
            connectionApi = connectionApiResult
                .onFailure { t -> error(t) { "Could not build connectionApi" } }
                .getOrElse { throw WrappedConnectionException(it) }
        }
    }

    private fun awaitCompletion() {
        scope.launchOnCompletion {
            info { "#init disconnecting connectionApi" }
            connectionApi?.disconnect()
        }
    }

    init {
        initConnectionApi()
        awaitCompletion()
    }

    override suspend fun onStatusUpdate(status: FInternalTransportConnectionStatus) {
        info { "#onStatusUpdate $status" }
        stateFlow.emit(status)
        yield() // Allow collectors to process the state before returning
    }

    suspend fun disconnect() {
        info { "#disconnect" }
        scope.cancel()
    }
}
