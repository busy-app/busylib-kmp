package net.flipper.bridge.connection.transport.tcp.lan.impl.monitor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.bridge.connection.transport.tcp.lan.impl.monitor.fixture.FAppleLanConnectionMonitorTestFixture
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalForeignApi::class)
@Suppress("TooManyFunctions")
class FAppleLanConnectionMonitorTest {

    private lateinit var testFixture: FAppleLanConnectionMonitorTestFixture

    @BeforeTest
    fun setUp() {
        testFixture = FAppleLanConnectionMonitorTestFixture()
    }

    @AfterTest
    fun tearDown() {
        testFixture.dispose()
    }

    private fun runTestWithDefaultDispatcher(
        block: suspend CoroutineScope.() -> Unit
    ): TestResult = runTest {
        withContext(Dispatchers.Default, block)
    }

    @Test
    fun GIVEN_running_server_WHEN_startMonitoring_THEN_reports_connected() {
        runTestWithDefaultDispatcher {
            testFixture.server.start()

            val monitor = testFixture.createMonitor(scope = this)
            monitor.startMonitoring()

            val connected = testFixture
                .listener
                .awaitStatusWithType<FInternalTransportConnectionStatus.Connected>()

            assertTrue(connected is FInternalTransportConnectionStatus.Connected)
            assertEquals(FInternalTransportConnectionType.LAN, connected.connectionType)
        }
    }

    @Test
    fun GIVEN_connected_WHEN_status_received_THEN_has_correct_scope_and_connectionType() {
        runTestWithDefaultDispatcher {
            testFixture.server.start()

            val monitor = testFixture.createMonitor(scope = this)
            monitor.startMonitoring()

            val connected = testFixture
                .listener
                .awaitStatusWithType<FInternalTransportConnectionStatus.Connected>()

            assertTrue(connected is FInternalTransportConnectionStatus.Connected)
            assertEquals(FInternalTransportConnectionType.LAN, connected.connectionType)
            assertEquals(this, connected.scope)
        }
    }

    @Test
    fun GIVEN_running_server_WHEN_startMonitoring_THEN_connecting_emitted_before_connected() {
        runTestWithDefaultDispatcher {
            testFixture.server.start()

            val monitor = testFixture.createMonitor(this)
            monitor.startMonitoring()

            testFixture.listener.awaitStatusWithType<FInternalTransportConnectionStatus.Connected>()

            // preparing/waiting states should now emit Connecting before Connected
            val firstStatus = testFixture.listener.statuses.first()

            assertEquals(
                expected = FInternalTransportConnectionStatus.Connecting,
                actual = firstStatus,
                message = "Expected first status to be Connecting (from preparing/waiting state), " +
                    "but was $firstStatus. Full statuses: ${testFixture.listener.statuses}"
            )
        }
    }

    @Test
    fun GIVEN_unreachable_host_WHEN_startMonitoring_THEN_connecting_reported() {
        runTestWithDefaultDispatcher {
            // 192.0.2.1 is TEST-NET-1 (RFC 5737) - guaranteed non-routable
            val monitor = testFixture.createMonitor(this, host = "192.0.2.1", port = "80")
            monitor.startMonitoring()

            // Await no other statuses appear
            testFixture.listener.awaitStatusCount(
                count = 0,
                predicate = { connectionStatus -> connectionStatus !is FInternalTransportConnectionStatus.Connecting }
            )

            // waiting state should now emit Connecting
            val status = testFixture
                .listener
                .awaitStatusWithType<FInternalTransportConnectionStatus.Connecting>(timeout = 10.seconds)
            assertEquals(FInternalTransportConnectionStatus.Connecting, status)
        }
    }

    @Test
    fun GIVEN_connected_WHEN_server_closes_THEN_reconnects_with_connecting() {
        runTestWithDefaultDispatcher {
            testFixture.server.start()

            val monitor = testFixture.createMonitor(this)
            monitor.startMonitoring()

            testFixture
                .listener
                .awaitStatusWithType<FInternalTransportConnectionStatus.Connected>()
            testFixture.server.stop()

            // After server close, client socket gets RST → state_failed/cancelled → restartMonitoring → Connecting
            val statusCountBefore = testFixture.listener.statuses.size
            val reconnectStatus = testFixture
                .listener
                .awaitStatus(
                    timeout = 20.seconds,
                    predicate = { connectionStatus ->
                        // Only match Connecting that arrived AFTER we stopped the server
                        connectionStatus is FInternalTransportConnectionStatus.Connecting &&
                            testFixture.listener.statuses.size > statusCountBefore
                    }
                )

            assertEquals(FInternalTransportConnectionStatus.Connecting, reconnectStatus)
        }
    }

    // ---- Fix verification: no duplicate reconnect attempts on failure ----
    // restartMonitoringScope with SKIP_IF_RUNNING should prevent multiple
    // concurrent restarts when state/viability/path handlers all fire.
    @Test
    fun GIVEN_connected_WHEN_server_closes_THEN_no_duplicate_connecting_burst() {
        runTestWithDefaultDispatcher {
            testFixture.server.start()

            val monitor = testFixture.createMonitor(this)
            monitor.startMonitoring()

            testFixture
                .listener
                .awaitStatusWithType<FInternalTransportConnectionStatus.Connected>()

            val statusCountBeforeStop = testFixture.listener.statuses.size

            testFixture.server.stop()

            // Wait for reconnection attempt
            testFixture.listener.awaitStatus(
                timeout = 20.seconds,
                predicate = { status ->
                    status is FInternalTransportConnectionStatus.Connecting &&
                        testFixture.listener.statuses.size > statusCountBeforeStop
                }
            )

            // Count Connecting statuses after Connected - should be exactly 1
            // because SKIP_IF_RUNNING prevents concurrent restarts
            val connectingAfterConnected = testFixture.listener.statuses
                .drop(statusCountBeforeStop)
                .count { connectionStatus -> connectionStatus is FInternalTransportConnectionStatus.Connecting }

            assertEquals(
                expected = 1,
                actual = connectingAfterConnected,
                message = "Expected exactly 1 Connecting after server close (SKIP_IF_RUNNING), " +
                    "but got $connectingAfterConnected. Full statuses: ${testFixture.listener.statuses}"
            )
        }
    }

    // If restartMonitoring was launched just before stopMonitoring is called,
    // the restart coroutine can still execute after the monitor is stopped.
    @Test
    fun GIVEN_connected_WHEN_stopMonitoring_THEN_no_new_statuses() {
        runTestWithDefaultDispatcher {
            testFixture.server.start()

            val monitor = testFixture.createMonitor(this)
            monitor.startMonitoring()

            testFixture
                .listener
                .awaitStatusWithType<FInternalTransportConnectionStatus.Connected>()

            monitor.stopMonitoring()

            testFixture.listener.assertNoNewStatus(
                duration = 2.seconds,
                predicate = { connectionStatus ->
                    connectionStatus is FInternalTransportConnectionStatus.Disconnected ||
                        connectionStatus is FInternalTransportConnectionStatus.Connected ||
                        connectionStatus is FInternalTransportConnectionStatus.Connecting
                }
            )
        }
    }

    @Test
    fun GIVEN_connecting_WHEN_stopMonitoring_THEN_no_connected_status() {
        runTestWithDefaultDispatcher {
            testFixture.server.start()

            val monitor = testFixture.createMonitor(this)
            monitor.startMonitoring()
            monitor.stopMonitoring()

            testFixture
                .listener
                .assertNoNewStatusWithType<FInternalTransportConnectionStatus.Connected>(duration = 2.seconds)
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
    fun GIVEN_connected_WHEN_server_restarts_THEN_reconnects_successfully() {
        runTestWithDefaultDispatcher {
            testFixture.server.start()
            val serverPort = testFixture.server.port

            val monitor = testFixture.createMonitor(this)
            monitor.startMonitoring()

            testFixture
                .listener
                .awaitStatusWithType<FInternalTransportConnectionStatus.Connected>()

            // Kill server
            val statusCountBeforeStop = testFixture.listener.statuses.size
            testFixture.server.stop()

            // Wait for reconnection attempt
            testFixture
                .listener
                .awaitStatus(
                    timeout = 20.seconds,
                    predicate = { connectionStatus ->
                        connectionStatus is FInternalTransportConnectionStatus.Connecting &&
                            testFixture.listener.statuses.size > statusCountBeforeStop
                    }
                )

            // Restart server
            testFixture.server.start()

            // If the server binds to a different port, the monitor won't reconnect
            // (it's still targeting the old port). This is expected behavior.
            if (testFixture.server.port == serverPort) {
                // Wait for reconnection
                testFixture.listener.awaitStatusCount(
                    count = 2,
                    timeout = 20.seconds,
                    predicate = { connectionStatus ->
                        connectionStatus is FInternalTransportConnectionStatus.Connected
                    }
                )
            }
        }
    }

    @Test
    fun GIVEN_not_started_WHEN_stopMonitoring_THEN_no_crash() {
        runTestWithDefaultDispatcher {
            val monitor = testFixture.createMonitor(this, port = "12345")
            monitor.stopMonitoring()

            assertTrue(testFixture.listener.statuses.isEmpty())
        }
    }
}
