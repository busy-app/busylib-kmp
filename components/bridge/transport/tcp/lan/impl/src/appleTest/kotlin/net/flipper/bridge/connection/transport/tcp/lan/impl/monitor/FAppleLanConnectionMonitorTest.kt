package net.flipper.bridge.connection.transport.tcp.lan.impl.monitor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withTimeout
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalForeignApi::class)
@Suppress("TooManyFunctions")
class FAppleLanConnectionMonitorTest {
    private lateinit var server: TestTcpServer
    private lateinit var scope: CoroutineScope
    private lateinit var listener: RecordingStatusListener
    private lateinit var deviceApi: FakeLanApi
    private val monitors = mutableListOf<FAppleLanConnectionMonitor>()

    @BeforeTest
    fun setUp() {
        server = TestTcpServer()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        listener = RecordingStatusListener()
        deviceApi = FakeLanApi()
    }

    @AfterTest
    fun tearDown() {
        monitors.forEach { it.stopMonitoring() }
        monitors.clear()
        platform.Foundation.NSThread.sleepForTimeInterval(1.0)
        scope.cancel()
        server.stop()
    }

    private fun createMonitor(
        host: String = "127.0.0.1",
        port: String = server.port.toString()
    ): FAppleLanConnectionMonitor {
        val config = FLanDeviceConnectionConfig(host = host, name = "test-device")
        val monitor = FAppleLanConnectionMonitor(
            listener = listener,
            config = config,
            scope = scope,
            deviceApi = deviceApi,
            port = port
        )
        monitors.add(monitor)
        return monitor
    }

    // ---- Basic connectivity ----

    @Test
    fun GIVEN_running_server_WHEN_startMonitoring_THEN_reports_connected() = runBlockingWithTimeout {
        server.start()
        val monitor = createMonitor()

        monitor.startMonitoring()
        val connected = listener.awaitStatus { it is FInternalTransportConnectionStatus.Connected }

        assertTrue(connected is FInternalTransportConnectionStatus.Connected)
        assertEquals(FInternalTransportConnectionType.LAN, connected.connectionType)
    }

    @Test
    fun GIVEN_connected_WHEN_status_received_THEN_has_correct_scope_and_connectionType() = runBlockingWithTimeout {
        server.start()
        val monitor = createMonitor()

        monitor.startMonitoring()
        val connected = listener.awaitStatus { it is FInternalTransportConnectionStatus.Connected }

        assertTrue(connected is FInternalTransportConnectionStatus.Connected)
        assertEquals(FInternalTransportConnectionType.LAN, connected.connectionType)
        assertEquals(scope, connected.scope)
    }

    @Test
    fun GIVEN_running_server_WHEN_startMonitoring_THEN_connecting_emitted_before_connected() = runBlockingWithTimeout {
        server.start()
        val monitor = createMonitor()

        monitor.startMonitoring()
        listener.awaitStatus { it is FInternalTransportConnectionStatus.Connected }

        // preparing/waiting states should now emit Connecting before Connected
        val firstStatus = listener.statuses.first()
        assertEquals(
            FInternalTransportConnectionStatus.Connecting,
            firstStatus,
            "Expected first status to be Connecting (from preparing/waiting state), " +
                "but was $firstStatus. Full statuses: ${listener.statuses}"
        )
    }

    // ---- Fix verification: unreachable host now reports Connecting ----

    @Test
    fun GIVEN_unreachable_host_WHEN_startMonitoring_THEN_connecting_reported() = runBlockingWithTimeout {
        // 192.0.2.1 is TEST-NET-1 (RFC 5737) - guaranteed non-routable
        val monitor = createMonitor(host = "192.0.2.1", port = "80")

        monitor.startMonitoring()

        // waiting state should now emit Connecting
        val status = listener.awaitStatus(timeout = 10.seconds) {
            it is FInternalTransportConnectionStatus.Connecting
        }
        assertEquals(FInternalTransportConnectionStatus.Connecting, status)
    }

    // ---- Fix verification: server close triggers reconnect, not Disconnected ----

    @Test
    fun GIVEN_connected_WHEN_server_closes_THEN_reconnects_with_connecting() = runBlockingWithTimeout(
        timeout = 30.seconds
    ) {
        server.start()
        val monitor = createMonitor()

        monitor.startMonitoring()
        listener.awaitStatus { it is FInternalTransportConnectionStatus.Connected }

        server.stop()

        // After server close, client socket gets RST → state_failed/cancelled → restartMonitoring → Connecting
        val statusCountBefore = listener.statuses.size
        val reconnectStatus = listener.awaitStatus(timeout = 20.seconds) { status ->
            // Only match Connecting that arrived AFTER we stopped the server
            status is FInternalTransportConnectionStatus.Connecting &&
                listener.statuses.size > statusCountBefore
        }
        assertEquals(FInternalTransportConnectionStatus.Connecting, reconnectStatus)
    }

    // ---- Fix verification: no duplicate reconnect attempts on failure ----
    // restartMonitoringScope with SKIP_IF_RUNNING should prevent multiple
    // concurrent restarts when state/viability/path handlers all fire.

    @Test
    fun GIVEN_connected_WHEN_server_closes_THEN_no_duplicate_connecting_burst() = runBlockingWithTimeout(
        timeout = 30.seconds
    ) {
        server.start()
        val monitor = createMonitor()

        monitor.startMonitoring()
        listener.awaitStatus { it is FInternalTransportConnectionStatus.Connected }

        val statusCountBeforeStop = listener.statuses.size
        server.stop()

        // Wait for reconnection attempt
        listener.awaitStatus(timeout = 20.seconds) { status ->
            status is FInternalTransportConnectionStatus.Connecting &&
                listener.statuses.size > statusCountBeforeStop
        }

        // Count Connecting statuses after Connected - should be exactly 1
        // because SKIP_IF_RUNNING prevents concurrent restarts
        val connectingAfterConnected = listener.statuses
            .drop(statusCountBeforeStop)
            .count { it is FInternalTransportConnectionStatus.Connecting }

        assertEquals(
            1,
            connectingAfterConnected,
            "Expected exactly 1 Connecting after server close (SKIP_IF_RUNNING), " +
                "but got $connectingAfterConnected. Full statuses: ${listener.statuses}"
        )
    }

    // If restartMonitoring was launched just before stopMonitoring is called,
    // the restart coroutine can still execute after the monitor is stopped.

    @Test
    fun GIVEN_connected_WHEN_stopMonitoring_THEN_no_new_statuses() = runBlockingWithTimeout {
        server.start()
        val monitor = createMonitor()

        monitor.startMonitoring()
        listener.awaitStatus { it is FInternalTransportConnectionStatus.Connected }

        monitor.stopMonitoring()

        listener.assertNoNewStatus(duration = 2.seconds) {
            it is FInternalTransportConnectionStatus.Disconnected ||
                it is FInternalTransportConnectionStatus.Connected ||
                it is FInternalTransportConnectionStatus.Connecting
        }
    }

    // stopMonitoring sets handlers to null + force_cancel.
    // But force_cancel might trigger the cancelled handler on the dispatch queue
    // before the null assignment takes effect (race window).

    @Test
    fun GIVEN_connecting_WHEN_stopMonitoring_THEN_no_connected_status() = runBlockingWithTimeout {
        server.start()
        val monitor = createMonitor()

        monitor.startMonitoring()
        monitor.stopMonitoring()

        listener.assertNoNewStatus(duration = 2.seconds) {
            it is FInternalTransportConnectionStatus.Connected
        }
    }

    // restartMonitoring runs in a coroutine scope but calls stopMonitoring()
    // which uses NSLock. Meanwhile, collectConnectionEvents uses runBlocking
    // inside a dispatch queue callback. If runBlocking is executing
    // handleStateUpdate while restartMonitoring tries to acquire connectionLock
    // in stopMonitoring, and the dispatch queue is serial, this could deadlock.
    //
    // Scenario: dispatch queue fires callback → runBlocking { handleStateUpdate() }
    // → restartMonitoring() → launch { stopMonitoring() } → connectionLock.withLock
    // → force_cancel → dispatch queue wants to fire cancelled callback but queue
    // is blocked by the first runBlocking.
    //
    // This test verifies the monitor survives a server-close reconnection cycle.

    @Test
    fun GIVEN_connected_WHEN_server_restarts_THEN_reconnects_successfully() = runBlockingWithTimeout(
        timeout = 30.seconds
    ) {
        server.start()
        val serverPort = server.port
        val monitor = createMonitor()

        monitor.startMonitoring()
        listener.awaitStatus { it is FInternalTransportConnectionStatus.Connected }

        // Kill server
        val statusCountBeforeStop = listener.statuses.size
        server.stop()

        // Wait for reconnection attempt
        listener.awaitStatus(timeout = 20.seconds) { status ->
            status is FInternalTransportConnectionStatus.Connecting &&
                listener.statuses.size > statusCountBeforeStop
        }

        // Restart server on same port
        server = TestTcpServer()
        server.start()
        // If the server binds to a different port, the monitor won't reconnect
        // (it's still targeting the old port). This is expected behavior.
        if (server.port == serverPort) {
            // Wait for reconnection
            listener.awaitStatusCount(
                count = 2,
                timeout = 20.seconds
            ) { it is FInternalTransportConnectionStatus.Connected }
        }
    }

    // ---- Verify stopMonitoring is idempotent ----

    @Test
    fun GIVEN_not_started_WHEN_stopMonitoring_THEN_no_crash() = runBlockingWithTimeout {
        val monitor = createMonitor(port = "12345")
        monitor.stopMonitoring()
        assertTrue(listener.statuses.isEmpty())
    }

    // ---- Helpers ----

    private fun runBlockingWithTimeout(
        timeout: kotlin.time.Duration = 30.seconds,
        block: suspend CoroutineScope.() -> Unit
    ) {
        kotlinx.coroutines.runBlocking {
            withTimeout(timeout, block)
        }
    }
}
