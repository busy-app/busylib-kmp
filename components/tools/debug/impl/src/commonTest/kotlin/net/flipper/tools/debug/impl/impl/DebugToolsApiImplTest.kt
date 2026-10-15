@file:Suppress("FunctionNaming")

package net.flipper.tools.debug.impl.impl

import kotlinx.coroutines.test.runTest
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.tools.debug.api.api.LocalStorageResetter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class DebugToolsApiImplTest {
    private class FakeResetter(private val failure: Throwable? = null) : LocalStorageResetter {
        var resetCount = 0

        override suspend fun reset() {
            resetCount++
            if (failure != null) throw failure
        }
    }

    @Test
    fun GIVEN_resetters_WHEN_local_storage_is_reset_THEN_every_resetter_runs_and_result_is_success() = runTest {
        val first = FakeResetter()
        val second = FakeResetter()
        val api = DebugToolsApiImpl(setOf(first, second))

        val result = api.resetLocalStorage()

        assertEquals(CResult.success(Unit), result)
        assertEquals(1, first.resetCount)
        assertEquals(1, second.resetCount)
    }

    @Test
    fun GIVEN_failing_resetter_WHEN_local_storage_is_reset_THEN_others_still_run_and_failure_is_returned() = runTest {
        val failure = IllegalStateException("boom")
        val failing = FakeResetter(failure)
        val healthy = FakeResetter()
        val api = DebugToolsApiImpl(setOf(failing, healthy))

        val result = api.resetLocalStorage()

        assertIs<CResult.Failure>(result)
        assertSame(failure, result.error)
        assertEquals(1, healthy.resetCount)
    }
}
