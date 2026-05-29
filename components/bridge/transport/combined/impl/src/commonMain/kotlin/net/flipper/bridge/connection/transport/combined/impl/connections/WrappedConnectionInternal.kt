package net.flipper.bridge.connection.transport.combined.impl.connections

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.combined.impl.connections.utils.ChildSupervisorScope
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalDisconnectedReason
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.common.api.FailedPairingConnectException
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.ktx.common.launchOnCompletion
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.warn

internal class WrappedConnectionInternal(
    private val config: FDeviceConnectionConfig<*>,
    parentScope: CoroutineScope,
    private val connectionBuilder: FDeviceConfigToConnection,
    private val dispatcher: CoroutineDispatcher = FlipperDispatchers.default
) : LogTagProvider, FTransportConnectionStatusListener {
    override val TAG = "WrappedConnection"

    private var connectionApi: FConnectedDeviceApi? = null
    val stateFlow: StateFlow<FInternalTransportConnectionStatus>
        field = MutableStateFlow<FInternalTransportConnectionStatus>(
            getConnectingStatus()
        )
    val stateFlowNotDisconnected: StateFlow<FInternalTransportConnectionStatus>
        field = MutableStateFlow<FInternalTransportConnectionStatus>(
            getConnectingStatus()
        )

    private val scope = ChildSupervisorScope(
        parentScope = parentScope,
        dispatcher = dispatcher,
        onCompletion = { t ->
            val recovery = when (t) {
                null -> {
                    info { "Wrapped connection $config scope is being cancelled" }
                    FInternalDisconnectedReason.OTHER
                }

                is CancellationException -> {
                    info { "Wrapped connection $config scope was cancelled" }
                    FInternalDisconnectedReason.OTHER
                }

                is FailedPairingConnectException -> {
                    info { "Wrapped connection $config failed with a not recoverable error" }
                    FInternalDisconnectedReason.REQUIRES_REPAIRING
                }

                else -> {
                    error(t) { "Scope for connection $config was cancelled due to an error" }
                    FInternalDisconnectedReason.OTHER
                }
            }
            updateStatus(FInternalTransportConnectionStatus.Disconnected(recovery))
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
            val exception = connectionApiResult.exceptionOrNull()
            if (exception is CancellationException) {
                throw exception
            }
            connectionApi = connectionApiResult
                .onFailure { t -> error(t) { "Could not build connectionApi" } }
                .getOrThrow()
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
        if (!scope.isActive) {
            warn { "Call #onStatusUpdate after scope is dead" }
            return
        }
        updateStatus(status)
        yield() // Allow collectors to process the state before returning
    }

    private fun updateStatus(status: FInternalTransportConnectionStatus) {
        if (stateFlow.value is FInternalTransportConnectionStatus.Disconnected) {
            warn { "Status updates are not permitted after the 'Disconnected' status" }
            return
        }
        // Combined transport keeps recoverable reconnects private and reports them as Connecting.
        stateFlowNotDisconnected.update { status.toNotDisconnectedStatus() }
        stateFlow.update { status }
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

    suspend fun disconnect() {
        info { "#disconnect" }
        scope.cancel()
    }
}
