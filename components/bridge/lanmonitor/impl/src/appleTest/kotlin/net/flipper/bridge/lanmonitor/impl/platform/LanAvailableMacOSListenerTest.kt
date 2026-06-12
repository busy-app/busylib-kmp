package net.flipper.bridge.lanmonitor.impl.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.flipper.bridge.lanmonitor.impl.platform.fixture.LanAvailableMacOSListenerTestFixture
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Behavioural adaptation of `FAppleLanConnectionMonitorTest` (from the tcp/lan transport module) to the
 * LAN-monitor module's [LanAvailableMacOSListener], whose contract is a `Flow<Boolean>`:
 * `true` = device reachable (LAN available), `false` = preparing / waiting / restarting.
 *
 * Tests that depended on the old listener-status object (scope / connectionType) or on a public stop API
 * are intentionally omitted — neither exists in the Boolean-flow contract, which always auto-starts in `init`.
 */
@OptIn(ExperimentalForeignApi::class)
class LanAvailableMacOSListenerTest {

    private lateinit var testFixture: LanAvailableMacOSListenerTestFixture

    @BeforeTest
    fun setUp() {
        testFixture = LanAvailableMacOSListenerTestFixture()
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
    fun GIVEN_running_server_WHEN_monitoring_THEN_emits_available() {
        runTestWithDefaultDispatcher {
            testFixture.server.start()

            val recorder = testFixture.createListener()

            val available = recorder.awaitValue { isAvailable -> isAvailable }
            assertTrue(available)
        }
    }

    @Test
    fun GIVEN_running_server_WHEN_monitoring_THEN_unavailable_emitted_before_available() {
        runTestWithDefaultDispatcher {
            testFixture.server.start()

            val recorder = testFixture.createListener()

            recorder.awaitValue { isAvailable -> isAvailable }

            // preparing/waiting states emit `false` before the connection becomes ready (`true`)
            val firstValue = recorder.values.first()
            assertEquals(
                expected = false,
                actual = firstValue,
                message = "Expected first emission to be false (preparing/waiting) before true. " +
                    "Full values: ${recorder.values}"
            )
        }
    }

    @Test
    fun GIVEN_unreachable_host_WHEN_monitoring_THEN_stays_unavailable() {
        runTestWithDefaultDispatcher {
            // 192.0.2.1 is TEST-NET-1 (RFC 5737) - guaranteed non-routable
            val recorder = testFixture.createListener(host = "192.0.2.1", port = 80)

            // preparing/waiting emit `false`...
            recorder.awaitValue(timeout = 10.seconds) { isAvailable -> !isAvailable }
            // ...but the connection must never become available
            recorder.assertNoNewValue(duration = 2.seconds) { isAvailable -> isAvailable }
        }
    }

    @Test
    fun GIVEN_available_WHEN_server_closes_THEN_emits_unavailable() {
        runTestWithDefaultDispatcher {
            testFixture.server.start()

            val recorder = testFixture.createListener()

            recorder.awaitValue { isAvailable -> isAvailable }
            val countBeforeClose = recorder.values.size
            testFixture.server.stop()

            // After server close, client socket gets RST → failed/cancelled → restartMonitoring → emits `false`
            val unavailable = recorder.awaitValue(timeout = 20.seconds) { isAvailable ->
                !isAvailable && recorder.values.size > countBeforeClose
            }
            assertEquals(false, unavailable)
        }
    }

    // Fix verification: no duplicate restarts on failure.
    // restartMonitoringScope with SKIP_IF_RUNNING should prevent multiple concurrent restarts
    // when the state/viability/path handlers all fire on server close, so exactly one `false` is emitted.
    @Test
    fun GIVEN_available_WHEN_server_closes_THEN_no_duplicate_unavailable_burst() {
        runTestWithDefaultDispatcher {
            testFixture.server.start()

            val recorder = testFixture.createListener()

            recorder.awaitValue { isAvailable -> isAvailable }
            val countAfterConnected = recorder.values.size

            testFixture.server.stop()

            // Wait for the restart-driven `false`
            recorder.awaitValue(timeout = 20.seconds) { isAvailable ->
                !isAvailable && recorder.values.size > countAfterConnected
            }

            // Count `false` values after Connected - should be exactly 1 because SKIP_IF_RUNNING
            // collapses the concurrent state/viability/path restart attempts into one.
            // The 5s restart delay means the re-connection's `preparing` (a second `false`) has not fired yet.
            val unavailableAfterConnected = recorder.values
                .drop(countAfterConnected)
                .count { isAvailable -> !isAvailable }

            assertEquals(
                expected = 1,
                actual = unavailableAfterConnected,
                message = "Expected exactly 1 false after server close (SKIP_IF_RUNNING), " +
                    "but got $unavailableAfterConnected. Full values: ${recorder.values}"
            )
        }
    }

    // restartMonitoring runs in a coroutine that calls cancelConnection() (NSLock), while
    // collectConnectionEvents uses runBlocking inside a dispatch-queue callback. If runBlocking is
    // executing handleStateUpdate while restartMonitoring tries to acquire connectionLock and the
    // dispatch queue is serial, this could deadlock. This test verifies the monitor survives a
    // server-close → reconnection cycle and reports availability again.
    @Test
    fun GIVEN_available_WHEN_server_restarts_THEN_reconnects() {
        runTestWithDefaultDispatcher {
            testFixture.server.start()
            val serverPort = testFixture.server.port

            val recorder = testFixture.createListener()

            recorder.awaitValue { isAvailable -> isAvailable }

            val countBeforeStop = recorder.values.size
            testFixture.server.stop()

            // Wait for reconnection attempt (`false`)
            recorder.awaitValue(timeout = 20.seconds) { isAvailable ->
                !isAvailable && recorder.values.size > countBeforeStop
            }

            testFixture.server.start()

            // If the server rebinds a different port, the listener won't reconnect (still targeting the
            // old port). This is expected behaviour, so only assert reconnection when the port is reused.
            if (testFixture.server.port == serverPort) {
                recorder.awaitValueCount(
                    count = 2,
                    timeout = 20.seconds,
                    predicate = { isAvailable -> isAvailable }
                )
            }
        }
    }
}
