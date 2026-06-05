package net.flipper.bridge.lanmonitor.impl.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.lanmonitor.impl.BB_HOST
import net.flipper.bridge.lanmonitor.impl.BB_PORT
import net.flipper.bridge.lanmonitor.impl.platform.model.KotlinNwStatus
import net.flipper.bridge.lanmonitor.impl.platform.model.asKotlinNwStatus
import net.flipper.bridge.lanmonitor.impl.platform.util.withLock
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
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
import platform.Network.nw_connection_t
import platform.Network.nw_endpoint_create_host
import platform.Network.nw_interface_type_other
import platform.Network.nw_parameters_copy_default_protocol_stack
import platform.Network.nw_parameters_create
import platform.Network.nw_parameters_prohibit_interface_type
import platform.Network.nw_path_get_status
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_protocol_stack_set_transport_protocol
import platform.Network.nw_tcp_create_options
import platform.Network.nw_tcp_options_set_enable_keepalive
import platform.Network.nw_tcp_options_set_keepalive_count
import platform.Network.nw_tcp_options_set_keepalive_idle_time
import platform.Network.nw_tcp_options_set_keepalive_interval
import platform.darwin.dispatch_queue_create
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.time.Duration.Companion.seconds

/**
 * Amount of seconds before sending keep-alive requests
 * @see KotlinNwStatus.PosixError.TimedOut
 */
private const val KEEPALIVE_IDLE_TIME_SEC = 5u

/**
 * Amount of seconds between keep-alive requests
 * @see KotlinNwStatus.PosixError.TimedOut
 */
private const val KEEPALIVE_INTERVAL_SEC = 3u

/**
 * Maximum numbers of unanswered keep-alive requests
 * @see KotlinNwStatus.PosixError.TimedOut
 */
private const val KEEPALIVE_COUNT = 3u
private val RESTART_TIMEOUT = 5.seconds

@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, LanAvailablePlatformListener::class)
class LanAvailableMacOSListener internal constructor(
    globalScope: CoroutineScope,
    private val host: String,
    private val port: Int,
) : LanAvailablePlatformListener, LogTagProvider {

    @Inject
    constructor(globalScope: CoroutineScope) : this(globalScope, BB_HOST, BB_PORT)

    override val TAG: String = "FAppleLanConnectionMonitor"
    private val queue = dispatch_queue_create("net.flipper.lan.connection", null)
    private val lanAvailableStateFlow = MutableSharedFlow<Boolean>()
    private val restartMonitoringScope = globalScope.asSingleJobScope()
    private val connectionLock = NSLock()
    private var connection: nw_connection_t = null

    override fun getLanAvailableFlow() = lanAvailableStateFlow

    init {
        startMonitoring()
    }

    private fun restartMonitoringUnsafe() {
        restartMonitoringScope.launch(SingleJobMode.SKIP_IF_RUNNING) {
            info { "#restartMonitoringUnsafe" }
            lanAvailableStateFlow.emit(false)
            cancelConnection()
            delay(RESTART_TIMEOUT)
            startMonitoring()
        }
    }

    private fun createConnectionUnsafe(): nw_connection_t {
        connection?.let { nw_connection_cancel(it) }

        val endpoint = nw_endpoint_create_host(host, port.toString())
        val parameters = nw_parameters_create()
        val protocolStack = nw_parameters_copy_default_protocol_stack(parameters)

        val tcpOptions = nw_tcp_create_options()
        nw_tcp_options_set_enable_keepalive(tcpOptions, true)
        nw_tcp_options_set_keepalive_idle_time(tcpOptions, KEEPALIVE_IDLE_TIME_SEC)
        nw_tcp_options_set_keepalive_interval(tcpOptions, KEEPALIVE_INTERVAL_SEC)
        nw_tcp_options_set_keepalive_count(tcpOptions, KEEPALIVE_COUNT)

        nw_protocol_stack_set_transport_protocol(protocolStack, tcpOptions)

        nw_parameters_prohibit_interface_type(parameters, nw_interface_type_other)

        val createdConnection = nw_connection_create(endpoint, parameters)
        connection = createdConnection
        return createdConnection
    }

    private suspend fun handleStateUpdateUnsafe(status: KotlinNwStatus) {
        when (status) {
            KotlinNwStatus.Ready -> {
                debug { "#handleStateUpdate Connected to $host" }
                lanAvailableStateFlow.emit(true)
            }

            KotlinNwStatus.Preparing -> {
                debug { "#handleStateUpdate Connection preparing" }
                lanAvailableStateFlow.emit(false)
            }

            is KotlinNwStatus.Waiting.LocalNetworkDenied,
            is KotlinNwStatus.Waiting.CellularDenied,
            is KotlinNwStatus.Waiting.WifiDenied -> {
                error { "#handleStateUpdate $status" }
                lanAvailableStateFlow.emit(false)
            }

            is KotlinNwStatus.Waiting.Other -> {
                debug { "#handleStateUpdate Waiting for connection: ${status.error}" }
                when (status.error) {
                    is KotlinNwStatus.PosixError.HostIsDown,
                    is KotlinNwStatus.PosixError.NoRouteToHost,
                    is KotlinNwStatus.PosixError.Unknown,
                    is KotlinNwStatus.PosixError.TimedOut,
                    is KotlinNwStatus.PosixError.ResetByPeer -> restartMonitoringUnsafe()

                    null -> {
                        lanAvailableStateFlow.emit(false)
                    }
                }
            }

            is KotlinNwStatus.UnknownState,
            is KotlinNwStatus.Cancelled,
            is KotlinNwStatus.Invalid,
            is KotlinNwStatus.Failed -> {
                error { "#handleStateUpdate $status" }
                restartMonitoringUnsafe()
            }
        }
    }

    private fun collectConnectionEventsUnsafe(connection: nw_connection_t) {
        nw_connection_set_state_changed_handler(connection) { state, error ->
            val status = connection.asKotlinNwStatus(state, error)
            debug { "#collectConnectionEvents status=$status (rawState=$state)" }
            runBlocking { handleStateUpdateUnsafe(status) }
        }
    }

    /**
     * Registers a handler that fires when TCP connection's viability changes
     * A connection becomes non-viable when it can no longer carry data
     * (e.g. USB cable disconnected or the device stopped responding)
     */
    private fun collectConnectionViabilityUnsafe(connection: nw_connection_t) {
        nw_connection_set_viability_changed_handler(connection) { isViable ->
            if (isViable) return@nw_connection_set_viability_changed_handler
            error { "#collectConnectionViability Connection became non-viable" }
            restartMonitoringUnsafe()
        }
    }

    /**
     * Registers a handler that fires when the underlying network path changes
     * The path describes which local interface and route is used for this TCP connection
     * (e.g. the USB-LAN interface went down)
     */
    private fun collectConnectionPathChangeUnsafe(connection: nw_connection_t) {
        nw_connection_set_path_changed_handler(connection) { path ->
            val status = nw_path_get_status(path)
            if (status == nw_path_status_satisfied) return@nw_connection_set_path_changed_handler
            error { "#collectConnectionPathChange Path no longer satisfied (status=$status)" }
            restartMonitoringUnsafe()
        }
    }

    private fun startMonitoring() {
        connectionLock.withLock {
            if (connection != null) {
                info { "#startMonitoring connection already exists" }
                return@withLock
            }

            val currentConnection = createConnectionUnsafe()

            collectConnectionEventsUnsafe(currentConnection)
            collectConnectionViabilityUnsafe(currentConnection)
            collectConnectionPathChangeUnsafe(currentConnection)

            nw_connection_set_queue(currentConnection, queue)
            nw_connection_start(currentConnection)

            info { "#startMonitoring Started monitoring connection to $host" }
        }
    }

    private fun cancelConnection() {
        connectionLock.withLock {
            connection?.let { localConnection ->
                nw_connection_set_path_changed_handler(localConnection, null)
                nw_connection_set_viability_changed_handler(localConnection, null)
                nw_connection_set_state_changed_handler(localConnection, null)

                nw_connection_force_cancel(localConnection)
            }
            connection = null
            info { "#stopMonitoring Stopped monitoring connection to $host" }
        }
    }
}
