package net.flipper.bridge.connection.transport.tcp.lan.impl.monitor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.common.monitor.FConnectionMonitorApi
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig
import net.flipper.bridge.connection.transport.tcp.lan.impl.model.KotlinNwError
import net.flipper.bridge.connection.transport.tcp.lan.impl.model.asKotlinNwError
import net.flipper.bridge.connection.transport.tcp.lan.impl.util.withLock
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
import platform.Network.nw_connection_state_cancelled
import platform.Network.nw_connection_state_failed
import platform.Network.nw_connection_state_invalid
import platform.Network.nw_connection_state_preparing
import platform.Network.nw_connection_state_ready
import platform.Network.nw_connection_state_waiting
import platform.Network.nw_connection_t
import platform.Network.nw_endpoint_create_address
import platform.Network.nw_endpoint_create_host
import platform.Network.nw_endpoint_t
import platform.Network.nw_parameters_copy_default_protocol_stack
import platform.Network.nw_parameters_create
import platform.Network.nw_parameters_set_prohibit_constrained
import platform.Network.nw_parameters_set_prohibit_expensive
import platform.Network.nw_path_get_status
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_protocol_stack_set_transport_protocol
import platform.Network.nw_tcp_create_options
import platform.Network.nw_tcp_options_set_enable_keepalive
import platform.Network.nw_tcp_options_set_keepalive_count
import platform.Network.nw_tcp_options_set_keepalive_idle_time
import platform.Network.nw_tcp_options_set_keepalive_interval
import platform.darwin.dispatch_queue_create
import platform.posix.AF_INET
import platform.posix.sockaddr_in

@OptIn(ExperimentalForeignApi::class)
class FAppleLanConnectionMonitor(
    private val listener: FTransportConnectionStatusListener,
    private val config: FLanDeviceConnectionConfig,
    private val scope: CoroutineScope,
    private val deviceApi: FConnectedDeviceApi,
    private val port: String = DEFAULT_PORT
) : FConnectionMonitorApi, LogTagProvider {
    override val TAG: String = "FAppleLanConnectionMonitor"
    private val queue = dispatch_queue_create("net.flipper.lan.connection", null)
    private val restartMonitoringScope = scope.asSingleJobScope()
    private val connectionLock = NSLock()
    private var connection: nw_connection_t = null

    private fun restartMonitoring() {
        restartMonitoringScope.launch(SingleJobMode.SKIP_IF_RUNNING) {
            listener.onStatusUpdate(FInternalTransportConnectionStatus.Connecting(config.getTransportTypes()))
            stopMonitoring()
            startMonitoring()
        }
    }

    /**
     * Parses [host] as a dotted-decimal IPv4 literal (e.g. `10.0.4.20`).
     *
     * Returns the address packed in **network byte order** (big-endian) ready
     * to be assigned to [sockaddr_in.sin_addr.s_addr], or `null` if [host] is
     * not a well-formed IPv4 literal.
     *
     * We deliberately do not use `inet_pton` here: on Apple targets it is
     * exposed as an `<arpa/inet.h>` symbol that is not always linkable from
     * Kotlin/Native cinterop, while the parsing logic is trivial.
     */
    private fun parseIpv4(host: String): UInt? {
        val parts = host.split('.')
        if (parts.size != IPV4_OCTET_COUNT) return null
        var network = 0u
        for (index in 0 until IPV4_OCTET_COUNT) {
            val octet = parts[index].toIntOrNull() ?: return null
            if (octet !in 0..IPV4_OCTET_MAX) return null
            // Network byte order = big-endian. The leftmost octet ("10" in
            // "10.0.4.20") goes into the **lowest** memory byte, which on
            // little-endian hosts means it lives in the **lowest** bits of the
            // resulting `UInt` because `s_addr` is interpreted byte-by-byte.
            network = network or (octet.toUInt() shl (BITS_PER_BYTE * index))
        }
        return network
    }

    /**
     * Swaps a [UShort] from host byte order to network byte order.
     *
     * Equivalent to libc's `htons`. Apple platforms are little-endian, so we
     * always swap; we don't bother detecting the host endianness because all
     * Kotlin/Native Apple targets are LE.
     */
    private fun hostToNetworkShort(value: UShort): UShort {
        val v = value.toInt() and 0xFFFF
        return ((v shl BITS_PER_BYTE) or (v ushr BITS_PER_BYTE) and 0xFFFF).toUShort()
    }

    /**
     * Builds an [nw_endpoint_t] that points at `config.host:port`.
     *
     * If `config.host` is a literal IPv4 address (the BUSY Bar's static
     * `10.0.4.20` by default) we build the endpoint from a raw [sockaddr_in]
     * via [nw_endpoint_create_address]. This skips Network.framework's host
     * resolution / DNS / proxy-auto-config pipeline, which would otherwise
     * route the connection over Wi-Fi through the system HTTP proxy / iCloud
     * Private Relay (MASQUE) and return `502 unreachable through proxy`.
     *
     * For non-IPv4 inputs we fall back to [nw_endpoint_create_host] (hostname
     * or IPv6 literal).
     */
    private fun createEndpoint(): nw_endpoint_t {
        val portNumber = port.toUShortOrNull() ?: return nw_endpoint_create_host(config.host, port)
        val ipv4 = parseIpv4(config.host) ?: return nw_endpoint_create_host(config.host, port)
        return memScoped {
            val addr = alloc<sockaddr_in>()
            addr.sin_len = sizeOf<sockaddr_in>().convert()
            addr.sin_family = AF_INET.convert()
            addr.sin_port = hostToNetworkShort(portNumber)
            addr.sin_addr.s_addr = ipv4
            nw_endpoint_create_address(addr.ptr.reinterpret())
        }
    }

    private fun createConnection(): nw_connection_t {
        return connectionLock.withLock {
            connection?.let { nw_connection_cancel(it) }

            val endpoint = createEndpoint()
            val parameters = nw_parameters_create()
            val protocolStack = nw_parameters_copy_default_protocol_stack(parameters)

            val tcpOptions = nw_tcp_create_options()
            nw_tcp_options_set_enable_keepalive(tcpOptions, true)
            nw_tcp_options_set_keepalive_idle_time(tcpOptions, KEEPALIVE_IDLE_TIME_SEC)
            nw_tcp_options_set_keepalive_interval(tcpOptions, KEEPALIVE_INTERVAL_SEC)
            nw_tcp_options_set_keepalive_count(tcpOptions, KEEPALIVE_COUNT)

            nw_protocol_stack_set_transport_protocol(protocolStack, tcpOptions)

            nw_parameters_set_prohibit_expensive(parameters, true)
            nw_parameters_set_prohibit_constrained(parameters, true)

            val createdConnection = nw_connection_create(endpoint, parameters)
            connection = createdConnection
            createdConnection
        }
    }

    private suspend fun handleStateUpdate(
        state: UInt,
        error: KotlinNwError?
    ) {
        when (error) {
            null,
            is KotlinNwError.HostIsDown,
            is KotlinNwError.Unknown,
            is KotlinNwError.NoRouteToHost -> Unit

            is KotlinNwError.TimedOut,
            is KotlinNwError.ResetByPeer -> {
                restartMonitoring()
                return
            }
        }
        when (state) {
            nw_connection_state_ready -> {
                debug { "#handleStateUpdate Connected to ${config.host}" }
                val status = FInternalTransportConnectionStatus.Connected(
                    scope = scope,
                    deviceApi = deviceApi,
                    connectionType = FInternalTransportConnectionType.LAN
                )
                listener.onStatusUpdate(status)
            }

            nw_connection_state_waiting -> {
                debug { "#handleStateUpdate Waiting for connection: $error" }
                listener.onStatusUpdate(FInternalTransportConnectionStatus.Connecting(config.getTransportTypes()))
            }

            nw_connection_state_preparing -> {
                debug { "#handleStateUpdate Connection preparing" }
                listener.onStatusUpdate(FInternalTransportConnectionStatus.Connecting(config.getTransportTypes()))
            }

            nw_connection_state_failed -> {
                error { "#handleStateUpdate Connection failed: $error" }
                restartMonitoring()
            }

            nw_connection_state_invalid -> {
                error { "#handleStateUpdate Connection invalid: $error" }
                restartMonitoring()
            }

            nw_connection_state_cancelled -> {
                error { "#handleStateUpdate Connection cancelled" }
                restartMonitoring()
            }

            else -> {
                debug { "#handleStateUpdate Connection unknown state: $state; error: $error" }
                restartMonitoring()
            }
        }
    }

    private fun collectConnectionEvents(connection: nw_connection_t) {
        nw_connection_set_state_changed_handler(connection) { state, error ->
            val kError = error?.asKotlinNwError()
            debug { "#collectConnectionEvents state=$state error=$kError" }
            runBlocking {
                handleStateUpdate(
                    state = state,
                    error = kError
                )
            }
        }
    }

    /**
     * Registers a handler that fires when TCP connection's viability changes
     * A connection becomes non-viable when it can no longer carry data
     * (e.g. USB cable disconnected or the device stopped responding)
     */
    private fun collectConnectionViability(connection: nw_connection_t) {
        nw_connection_set_viability_changed_handler(connection) { isViable ->
            if (isViable) return@nw_connection_set_viability_changed_handler
            error { "#collectConnectionViability Connection became non-viable" }
            restartMonitoring()
        }
    }

    /**
     * Registers a handler that fires when the underlying network path changes
     * The path describes which local interface and route is used for this TCP connection
     * (e.g. the USB-LAN interface went down)
     */
    private fun collectConnectionPathChange(connection: nw_connection_t) {
        nw_connection_set_path_changed_handler(connection) { path ->
            val status = nw_path_get_status(path)
            if (status == nw_path_status_satisfied) return@nw_connection_set_path_changed_handler
            error { "#collectConnectionPathChange Path no longer satisfied (status=$status)" }
            restartMonitoring()
        }
    }

    override suspend fun startMonitoring() {
        val currentConnection = createConnection()

        collectConnectionEvents(currentConnection)
        collectConnectionViability(currentConnection)
        collectConnectionPathChange(currentConnection)

        nw_connection_set_queue(currentConnection, queue)
        nw_connection_start(currentConnection)

        info { "#startMonitoring Started monitoring connection to ${config.host}" }
    }

    override fun stopMonitoring() {
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
        private const val DEFAULT_PORT = "80"

        /** Number of octets in an IPv4 address ("a.b.c.d"). */
        private const val IPV4_OCTET_COUNT = 4

        /** Maximum value of a single IPv4 octet. */
        private const val IPV4_OCTET_MAX = 255

        /** Number of bits in a single byte. */
        private const val BITS_PER_BYTE = 8

        /**
         * Amount of seconds before sending keep-alive requests
         * @see KotlinNwError.TimedOut
         */
        private const val KEEPALIVE_IDLE_TIME_SEC = 5u

        /**
         * Amount of seconds between keep-alive requests
         * @see KotlinNwError.TimedOut
         */
        private const val KEEPALIVE_INTERVAL_SEC = 3u

        /**
         * Maximum numbers of unanswered keep-alive requests
         * @see KotlinNwError.TimedOut
         */
        private const val KEEPALIVE_COUNT = 3u
    }
}
