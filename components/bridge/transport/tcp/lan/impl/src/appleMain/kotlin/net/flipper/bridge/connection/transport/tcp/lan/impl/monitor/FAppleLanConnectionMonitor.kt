package net.flipper.bridge.connection.transport.tcp.lan.impl.monitor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig
import net.flipper.bridge.connection.transport.tcp.lan.impl.model.KotlinNwError
import net.flipper.bridge.connection.transport.tcp.lan.impl.model.asKotlinNwError
import net.flipper.bridge.connection.transport.tcp.lan.impl.util.withLock
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.cancelPrevious
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import platform.Foundation.NSLock
import platform.Network.nw_connection_cancel
import platform.Network.nw_connection_create
import platform.Network.nw_connection_force_cancel
import platform.Network.nw_connection_set_path_changed_handler
import platform.Network.nw_connection_set_queue
import platform.Network.nw_connection_set_state_changed_handler
import platform.Network.nw_connection_set_viability_changed_handler
import platform.Network.nw_connection_start
import platform.Network.nw_connection_state_cancelled
import platform.Network.nw_connection_state_failed
import platform.Network.nw_connection_state_invalid
import platform.Network.nw_connection_state_preparing
import platform.Network.nw_connection_state_ready
import platform.Network.nw_connection_state_waiting
import platform.Network.nw_connection_t
import platform.Network.nw_endpoint_create_host
import platform.Network.nw_parameters_copy_default_protocol_stack
import platform.Network.nw_parameters_create
import platform.Network.nw_parameters_set_include_peer_to_peer
import platform.Network.nw_path_get_status
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_protocol_stack_set_transport_protocol
import platform.Network.nw_tcp_create_options
import platform.Network.nw_tcp_options_set_enable_keepalive
import platform.Network.nw_tcp_options_set_keepalive_count
import platform.Network.nw_tcp_options_set_keepalive_idle_time
import platform.Network.nw_tcp_options_set_keepalive_interval
import platform.darwin.dispatch_queue_create
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalForeignApi::class)
class FAppleLanConnectionMonitor(
    private val listener: FTransportConnectionStatusListener,
    private val config: FLanDeviceConnectionConfig,
    private val scope: CoroutineScope,
    private val deviceApi: FConnectedDeviceApi
) : FLanConnectionMonitorApi, LogTagProvider {
    override val TAG: String = "FLanConnectionMonitor"
    private val queue = dispatch_queue_create("net.flipper.lan.connection", null)
    private val connectionLock = NSLock()
    private val connectionTimeoutScope = scope.asSingleJobScope()
    private var connection: nw_connection_t = null

    private fun createConnection(): nw_connection_t {
        return connectionLock.withLock {
            connection?.let { nw_connection_cancel(it) }

            val endpoint = nw_endpoint_create_host(config.host, "80")
            val parameters = nw_parameters_create()
            val protocolStack = nw_parameters_copy_default_protocol_stack(parameters)

            val tcpOptions = nw_tcp_create_options()
            nw_tcp_options_set_enable_keepalive(tcpOptions, true)
            nw_tcp_options_set_keepalive_idle_time(tcpOptions, KEEPALIVE_IDLE_TIME_SEC)
            nw_tcp_options_set_keepalive_interval(tcpOptions, KEEPALIVE_INTERVAL_SEC)
            nw_tcp_options_set_keepalive_count(tcpOptions, KEEPALIVE_COUNT)

            nw_protocol_stack_set_transport_protocol(protocolStack, tcpOptions)
            nw_parameters_set_include_peer_to_peer(parameters, true)

            val createdConnection = nw_connection_create(endpoint, parameters)
            connection = createdConnection
            createdConnection
        }
    }

    private fun sendDisconnectStatusAfterTimeout() {
        connectionTimeoutScope.launch(SingleJobMode.CANCEL_PREVIOUS) {
            delay(CONNECTION_TIMEOUT_JOB)
            listener.onStatusUpdate(FInternalTransportConnectionStatus.Disconnected)
            debug { "#sendDisconnectStatusAfterTimeout Could not connect within timeout, restarting" }
        }
    }

    private suspend fun handleStateUpdate(
        state: UInt,
        error: KotlinNwError?
    ) {
        when (error) {
            is KotlinNwError.TimedOut,
            is KotlinNwError.CodeHostIsDown,
            is KotlinNwError.NoRouteToHost,
            is KotlinNwError.ResetByPeer -> {
                error { "#handleStateUpdate Received error $error" }
                sendDisconnectStatusAfterTimeout()
                return
            }

            is KotlinNwError.Unknown,
            null -> Unit
        }

        when (state) {
            nw_connection_state_ready -> {
                info { "#handleStateUpdate Connected to ${config.host}" }
                val status = FInternalTransportConnectionStatus.Connected(
                    scope = scope,
                    deviceApi = deviceApi,
                    connectionType = FInternalTransportConnectionType.LAN
                )
                listener.onStatusUpdate(status)
            }

            nw_connection_state_invalid -> {
                error { "#handleStateUpdate Connection invalid: $error" }
                sendDisconnectStatusAfterTimeout()
            }

            nw_connection_state_waiting -> {
                debug { "#handleStateUpdate Waiting for connection: $error" }
                sendDisconnectStatusAfterTimeout()
            }

            nw_connection_state_failed -> {
                error { "#handleStateUpdate Connection failed: $error" }
                sendDisconnectStatusAfterTimeout()
            }

            nw_connection_state_cancelled -> {
                error { "#handleStateUpdate Connection cancelled" }
                sendDisconnectStatusAfterTimeout()
            }

            nw_connection_state_preparing -> {
                debug { "#handleStateUpdate Connection preparing" }
                sendDisconnectStatusAfterTimeout()
            }

            else -> {
                debug { "#handleStateUpdate Connection unknown state: $state; error: $error" }
                sendDisconnectStatusAfterTimeout()
            }
        }
    }

    private fun collectConnectionEvents(connection: nw_connection_t) {
        nw_connection_set_state_changed_handler(connection) { state, error ->
            val kError = error?.asKotlinNwError()
            info { "#collectConnectionEvents state=$state error=$kError" }
            runBlocking {
                connectionTimeoutScope.cancelPrevious()
                handleStateUpdate(
                    state = state,
                    error = kError
                )
            }
        }
    }

    private fun collectConnectionViability(connection: nw_connection_t) {
        nw_connection_set_viability_changed_handler(connection) { isViable ->
            info { "#collectConnectionViability isViable=$isViable" }
            if (isViable) return@nw_connection_set_viability_changed_handler
            runBlocking {
                error { "#collectConnectionViability Connection became non-viable, marking disconnected" }
                sendDisconnectStatusAfterTimeout()
            }
        }
    }

    private fun collectConnectionPathChange(connection: nw_connection_t) {
        nw_connection_set_path_changed_handler(connection) { path ->
            val status = nw_path_get_status(path)
            info { "#collectConnectionPathChange status=$status" }
            if (status == nw_path_status_satisfied) return@nw_connection_set_path_changed_handler
            runBlocking {
                error { "#collectConnectionPathChange Path no longer satisfied (status=$status), marking disconnected" }
                sendDisconnectStatusAfterTimeout()
            }
        }
    }

    override suspend fun startMonitoring() {
        info { "#startMonitoring" }
        val currentConnection = createConnection()

        collectConnectionEvents(currentConnection)
        collectConnectionViability(currentConnection)
        collectConnectionPathChange(currentConnection)

        nw_connection_set_queue(currentConnection, queue)
        nw_connection_start(currentConnection)

        info { "#startMonitoring Started monitoring connection to ${config.host}" }
    }

    override fun stopMonitoring() {
        info { "#stopMonitoring" }
        connectionLock.withLock {
            connection?.let { localConnection ->
                nw_connection_set_path_changed_handler(localConnection, null)
                nw_connection_set_viability_changed_handler(localConnection, null)
                nw_connection_set_state_changed_handler(localConnection, null)
                nw_connection_force_cancel(localConnection)
            }
            connection = null
        }
        info { "#stopMonitoring Stopped monitoring connection to ${config.host}" }
    }

    companion object {
        private const val KEEPALIVE_IDLE_TIME_SEC = 5u
        private const val KEEPALIVE_INTERVAL_SEC = 3u
        private const val KEEPALIVE_COUNT = 3u

        private val CONNECTION_TIMEOUT_JOB = 5.seconds
    }
}
