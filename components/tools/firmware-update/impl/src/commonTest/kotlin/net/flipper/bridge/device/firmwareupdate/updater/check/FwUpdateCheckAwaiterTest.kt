package net.flipper.bridge.device.firmwareupdate.updater.check

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateCheckResult
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.busylib.core.wrapper.CResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class FwUpdateCheckAwaiterTest {

    /**
     * A check request that takes [REQUEST_DURATION], after which the device reports [results]
     * one per second
     */
    private fun TestScope.successfulCheck(
        state: MutableStateFlow<FwUpdateState>,
        vararg results: FwUpdateState
    ): suspend () -> CResult<Unit> = {
        delay(REQUEST_DURATION)
        backgroundScope.launch {
            results.forEach { result ->
                delay(1.seconds)
                state.value = result
            }
        }
        CResult.success(Unit)
    }

    @Test
    fun GIVEN_previous_result_in_state_WHEN_check_THEN_returns_new_result() = runTest {
        val state = MutableStateFlow<FwUpdateState>(FwUpdateState.CouldNotCheckUpdate)

        val result = FwUpdateCheckAwaiter.await(
            state = state,
            skipsCurrentState = true,
            startCheck = successfulCheck(
                state,
                FwUpdateState.CheckingVersion,
                FwUpdateState.NoUpdateAvailable
            )
        )

        assertEquals(FwUpdateCheckResult.NO_UPDATE_AVAILABLE, result)
    }

    @Test
    fun GIVEN_result_reported_while_request_runs_WHEN_check_THEN_returns_it() = runTest {
        val state = MutableStateFlow<FwUpdateState>(FwUpdateState.CouldNotCheckUpdate)

        val result = FwUpdateCheckAwaiter.await(state = state, skipsCurrentState = true) {
            state.value = FwUpdateState.UpdateAvailable
            delay(REQUEST_DURATION)
            CResult.success(Unit)
        }

        assertEquals(FwUpdateCheckResult.UPDATE_AVAILABLE, result)
        assertEquals(REQUEST_DURATION.inWholeMilliseconds, currentTime)
    }

    @Test
    fun GIVEN_request_fails_WHEN_check_THEN_returns_failed_without_waiting() = runTest {
        val state = MutableStateFlow<FwUpdateState>(FwUpdateState.NoUpdateAvailable)

        val result = FwUpdateCheckAwaiter.await(state = state, skipsCurrentState = false) {
            CResult.failure(IllegalStateException("Request failed"))
        }

        assertEquals(FwUpdateCheckResult.FAILED, result)
        assertEquals(0L, currentTime)
        assertEquals(0, state.subscriptionCount.value)
    }

    @Test
    fun GIVEN_no_result_WHEN_timeout_passes_after_request_THEN_returns_failed() = runTest {
        val state = MutableStateFlow<FwUpdateState>(FwUpdateState.NoUpdateAvailable)

        val result = FwUpdateCheckAwaiter.await(
            state = state,
            skipsCurrentState = true,
            startCheck = successfulCheck(state, FwUpdateState.CheckingVersion)
        )

        assertEquals(FwUpdateCheckResult.FAILED, result)
        assertEquals((REQUEST_DURATION + RESULT_TIMEOUT).inWholeMilliseconds, currentTime)
        assertEquals(0, state.subscriptionCount.value)
    }

    @Test
    fun GIVEN_result_right_before_timeout_WHEN_check_THEN_returns_result() = runTest {
        val state = MutableStateFlow<FwUpdateState>(FwUpdateState.Pending)

        val result = FwUpdateCheckAwaiter.await(state = state, skipsCurrentState = true) {
            delay(REQUEST_DURATION)
            backgroundScope.launch {
                delay(RESULT_TIMEOUT - 1.milliseconds)
                state.value = FwUpdateState.UpdateAvailable
            }
            CResult.success(Unit)
        }

        assertEquals(FwUpdateCheckResult.UPDATE_AVAILABLE, result)
    }

    @Test
    fun GIVEN_device_could_not_check_WHEN_check_THEN_returns_failed_at_once() = runTest {
        val state = MutableStateFlow<FwUpdateState>(FwUpdateState.Pending)

        val result = FwUpdateCheckAwaiter.await(
            state = state,
            skipsCurrentState = true,
            startCheck = successfulCheck(
                state,
                FwUpdateState.CheckingVersion,
                FwUpdateState.CouldNotCheckUpdate
            )
        )

        assertEquals(FwUpdateCheckResult.FAILED, result)
        assertEquals((REQUEST_DURATION + 2.seconds).inWholeMilliseconds, currentTime)
    }

    @Test
    fun GIVEN_battery_low_WHEN_check_THEN_returns_battery_low() = runTest {
        val state = MutableStateFlow<FwUpdateState>(FwUpdateState.Pending)

        val result = FwUpdateCheckAwaiter.await(
            state = state,
            skipsCurrentState = true,
            startCheck = successfulCheck(state, FwUpdateState.BatteryLow)
        )

        assertEquals(FwUpdateCheckResult.BATTERY_LOW, result)
    }

    @Test
    fun GIVEN_states_without_result_WHEN_check_THEN_waits_for_result() = runTest {
        val state = MutableStateFlow<FwUpdateState>(FwUpdateState.UpdateAvailable)

        val result = FwUpdateCheckAwaiter.await(
            state = state,
            skipsCurrentState = true,
            startCheck = successfulCheck(
                state,
                FwUpdateState.Pending,
                FwUpdateState.CheckingVersion,
                FwUpdateState.Updating,
                FwUpdateState.Preparing,
                FwUpdateState.DownloadFailure,
                FwUpdateState.Uploading(progress = 0.5f),
                FwUpdateState.Downloading(progress = 0.5f, isLanUpdate = false),
                FwUpdateState.NoUpdateAvailable
            )
        )

        assertEquals(FwUpdateCheckResult.NO_UPDATE_AVAILABLE, result)
        assertEquals((REQUEST_DURATION + 8.seconds).inWholeMilliseconds, currentTime)
    }

    @Test
    fun GIVEN_lan_and_settled_state_WHEN_check_THEN_sends_request_and_returns_current_state() = runTest {
        val state = MutableStateFlow<FwUpdateState>(FwUpdateState.NoUpdateAvailable)
        var requestCount = 0

        val result = FwUpdateCheckAwaiter.await(state = state, skipsCurrentState = false) {
            requestCount++
            delay(REQUEST_DURATION)
            CResult.success(Unit)
        }

        assertEquals(FwUpdateCheckResult.NO_UPDATE_AVAILABLE, result)
        assertEquals(1, requestCount)
        assertEquals(REQUEST_DURATION.inWholeMilliseconds, currentTime)
    }

    @Test
    fun GIVEN_lan_and_state_not_settled_WHEN_check_THEN_waits_for_result() = runTest {
        val state = MutableStateFlow<FwUpdateState>(FwUpdateState.Pending)

        val result = FwUpdateCheckAwaiter.await(
            state = state,
            skipsCurrentState = false,
            startCheck = successfulCheck(state, FwUpdateState.UpdateAvailable)
        )

        assertEquals(FwUpdateCheckResult.UPDATE_AVAILABLE, result)
    }

    @Test
    fun GIVEN_waiting_for_result_WHEN_caller_cancelled_THEN_stops_collecting_state() = runTest {
        val state = MutableStateFlow<FwUpdateState>(FwUpdateState.Pending)
        val job = launch {
            FwUpdateCheckAwaiter.await(
                state = state,
                skipsCurrentState = true,
                startCheck = successfulCheck(state)
            )
        }
        advanceTimeBy(REQUEST_DURATION + 1.seconds)
        runCurrent()
        assertEquals(1, state.subscriptionCount.value)

        job.cancelAndJoin()

        assertEquals(0, state.subscriptionCount.value)
    }

    private companion object {
        val REQUEST_DURATION = 1.seconds
        val RESULT_TIMEOUT = 10.seconds
    }
}
