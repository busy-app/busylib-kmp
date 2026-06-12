package net.flipper.bridge.lanmonitor.impl.platform

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LanAvailableDesktopListenerTest {

    private class FakeLanReachabilityProbe(
        var reachable: Boolean
    ) : LanReachabilityProbe {
        var probeCount = 0
            private set

        override suspend fun isReachable(): Boolean {
            probeCount++
            return reachable
        }
    }

    @Test
    fun GIVEN_device_reachable_WHEN_listener_starts_THEN_flow_emits_true() = runTest {
        val probe = FakeLanReachabilityProbe(reachable = true)
        val listener = LanAvailableDesktopListener(
            globalScope = backgroundScope,
            probe = probe
        )

        runCurrent()

        assertTrue(listener.getLanAvailableFlow().value)
    }

    @Test
    fun GIVEN_device_becomes_unreachable_WHEN_next_poll_runs_THEN_flow_emits_false() = runTest {
        val probe = FakeLanReachabilityProbe(reachable = true)
        val listener = LanAvailableDesktopListener(
            globalScope = backgroundScope,
            probe = probe
        )
        runCurrent()
        assertTrue(listener.getLanAvailableFlow().value)

        probe.reachable = false
        advanceTimeBy(LanAvailableDesktopListener.POLL_INTERVAL)
        runCurrent()

        assertFalse(listener.getLanAvailableFlow().value)
    }

    @Test
    fun GIVEN_listener_running_WHEN_intervals_elapse_THEN_probe_is_polled_each_interval() = runTest {
        val probe = FakeLanReachabilityProbe(reachable = true)
        LanAvailableDesktopListener(
            globalScope = backgroundScope,
            probe = probe
        )

        runCurrent()
        assertEquals(1, probe.probeCount)

        advanceTimeBy(LanAvailableDesktopListener.POLL_INTERVAL)
        runCurrent()
        assertEquals(2, probe.probeCount)

        advanceTimeBy(LanAvailableDesktopListener.POLL_INTERVAL)
        runCurrent()
        assertEquals(3, probe.probeCount)
    }
}
