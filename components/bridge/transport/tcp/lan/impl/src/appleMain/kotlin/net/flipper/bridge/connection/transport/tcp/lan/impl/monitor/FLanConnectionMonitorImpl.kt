package net.flipper.bridge.connection.transport.tcp.lan.impl.monitor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig
import net.flipper.bridge.connection.transport.tcp.lan.monitor.FLanConnectionMonitorApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import platform.Foundation.NSLock
import platform.Network.nw_connection_cancel
import platform.Network.nw_connection_create
import platform.Network.nw_connection_force_cancel
import platform.Network.nw_connection_set_queue
import platform.Network.nw_connection_set_state_changed_handler
import platform.Network.nw_connection_start
import platform.Network.nw_connection_state_cancelled
import platform.Network.nw_connection_state_failed
import platform.Network.nw_connection_state_preparing
import platform.Network.nw_connection_state_ready
import platform.Network.nw_connection_state_waiting
import platform.Network.nw_connection_t
import platform.Network.nw_endpoint_create_host
import platform.Network.nw_error_t
import platform.Network.nw_parameters_copy_default_protocol_stack
import platform.Network.nw_parameters_create
import platform.Network.nw_parameters_set_include_peer_to_peer
import platform.Network.nw_protocol_stack_set_transport_protocol
import platform.Network.nw_tcp_create_options
import platform.darwin.dispatch_queue_create
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@OptIn(ExperimentalForeignApi::class)
class FLanConnectionMonitorImpl(
    @Assisted private val listener: FTransportConnectionStatusListener,
    @Assisted private val config: FLanDeviceConnectionConfig
) : FLanConnectionMonitorApi, LogTagProvider {
    override val TAG: String = "FLanConnectionMonitor"
    private val queue = dispatch_queue_create("net.flipper.lan.connection", null)
    private val connectionLock = NSLock()
    private var connection: nw_connection_t? = null

    override fun startMonitoring(
        scope: CoroutineScope,
        deviceApi: FConnectedDeviceApi
    ) {
        val newConnection = connectionLock.withLock {
            connection?.let { nw_connection_cancel(it) }

            val endpoint = nw_endpoint_create_host(config.host, "80")
            val parameters = nw_parameters_create()
            val protocolStack = nw_parameters_copy_default_protocol_stack(parameters)
            val tcpOptions = nw_tcp_create_options()
            nw_protocol_stack_set_transport_protocol(protocolStack, tcpOptions)
            nw_parameters_set_include_peer_to_peer(parameters, true)

            val createdConnection = nw_connection_create(endpoint, parameters)
            connection = createdConnection
            createdConnection
        }

        nw_connection_set_state_changed_handler(newConnection) { state, error ->
            handleStateUpdate(
                state,
                error,
                scope,
                deviceApi
            )
        }

        nw_connection_set_queue(newConnection, queue)
        nw_connection_start(newConnection)

        info { "Started monitoring connection to ${config.host}" }
    }

    override fun stopMonitoring() {
        connectionLock.withLock {
            connection?.let { nw_connection_force_cancel(it) }
            connection = null
        }
        info { "Stopped monitoring connection to ${config.host}" }
    }

    private fun handleStateUpdate(
        state: UInt,
        error: nw_error_t?,
        scope: CoroutineScope,
        deviceApi: FConnectedDeviceApi
    ) {
        when (state) {
            nw_connection_state_ready -> {
                info { "Connected to ${config.host}" }
                listener.onStatusUpdate(
                    FInternalTransportConnectionStatus.Connected(
                        scope = scope,
                        deviceApi = deviceApi
                    )
                )
            }
            nw_connection_state_waiting -> {
                debug { "Waiting for connection: ${error?.toString()}" }
            }
            nw_connection_state_failed -> {
                error { "Connection failed: ${error?.toString()}" }
                listener.onStatusUpdate(FInternalTransportConnectionStatus.Disconnected)
            }
            nw_connection_state_cancelled -> {
                error { "Connection cancelled" }
                listener.onStatusUpdate(FInternalTransportConnectionStatus.Disconnected)
            }
            nw_connection_state_preparing -> {
                debug { "Connection preparing" }
            }
            else -> {
                debug { "Connection unknown state: $state" }
            }
        }
    }

    @Inject
    @ContributesBinding(BusyLibGraph::class, FLanConnectionMonitorApi.Factory::class)
    class InternalFactory(
        private val factory: (
            FTransportConnectionStatusListener,
            FLanDeviceConnectionConfig
        ) -> FLanConnectionMonitorImpl
    ) : FLanConnectionMonitorApi.Factory {
        override operator fun invoke(
            listener: FTransportConnectionStatusListener,
            config: FLanDeviceConnectionConfig
        ): FLanConnectionMonitorApi = factory(listener, config)
    }
}

private inline fun <T> NSLock.withLock(block: () -> T): T {
    lock()
    return try {
        block()
    } finally {
        unlock()
    }
}
