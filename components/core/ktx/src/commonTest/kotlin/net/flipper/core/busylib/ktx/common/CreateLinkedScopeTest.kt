package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreateLinkedScopeTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GIVEN_two_active_parents_WHEN_linked_scope_created_THEN_linked_is_active() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val first = CoroutineScope(Job(backgroundScope.coroutineContext.job))
        val second = CoroutineScope(Job(backgroundScope.coroutineContext.job))

        val linked = createLinkedScope(first, second, dispatcher)

        assertTrue(
            actual = linked.coroutineContext.job.isActive,
            message = "Linked scope should be active while both parents are active"
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GIVEN_linked_scope_WHEN_first_parent_cancelled_THEN_linked_is_cancelled_and_second_survives() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val first = CoroutineScope(Job(backgroundScope.coroutineContext.job))
        val second = CoroutineScope(Job(backgroundScope.coroutineContext.job))
        val linked = createLinkedScope(first, second, dispatcher)

        first.cancel()
        advanceUntilIdle()

        assertFalse(
            actual = linked.coroutineContext.job.isActive,
            message = "Linked scope should be cancelled when the first parent is cancelled"
        )
        assertTrue(
            actual = second.coroutineContext.job.isActive,
            message = "Second parent should remain active when the first parent is cancelled"
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GIVEN_linked_scope_WHEN_second_parent_cancelled_THEN_linked_is_cancelled_and_first_survives() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val first = CoroutineScope(Job(backgroundScope.coroutineContext.job))
        val second = CoroutineScope(Job(backgroundScope.coroutineContext.job))
        val linked = createLinkedScope(first, second, dispatcher)

        second.cancel()
        advanceUntilIdle()

        assertFalse(
            actual = linked.coroutineContext.job.isActive,
            message = "Linked scope should be cancelled when the second parent is cancelled"
        )
        assertTrue(
            actual = first.coroutineContext.job.isActive,
            message = "First parent should remain active when the second parent is cancelled"
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GIVEN_linked_scope_WHEN_linked_scope_cancelled_THEN_parents_remain_active() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val first = CoroutineScope(Job(backgroundScope.coroutineContext.job))
        val second = CoroutineScope(Job(backgroundScope.coroutineContext.job))
        val linked = createLinkedScope(first, second, dispatcher)

        linked.cancel()
        advanceUntilIdle()

        assertTrue(
            actual = first.coroutineContext.job.isActive,
            message = "First parent should remain active when the linked scope is cancelled"
        )
        assertTrue(
            actual = second.coroutineContext.job.isActive,
            message = "Second parent should remain active when the linked scope is cancelled"
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GIVEN_running_child_in_linked_scope_WHEN_parent_cancelled_THEN_child_is_cancelled() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val first = CoroutineScope(Job(backgroundScope.coroutineContext.job))
        val second = CoroutineScope(Job(backgroundScope.coroutineContext.job))
        val linked = createLinkedScope(first, second, dispatcher)

        val job = linked.launch { awaitCancellation() }

        first.cancel()
        advanceUntilIdle()

        assertTrue(
            actual = job.isCancelled,
            message = "Child coroutine of linked scope should be cancelled when a parent is cancelled"
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GIVEN_first_parent_already_cancelled_WHEN_linked_scope_created_THEN_linked_is_already_cancelled() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val first = CoroutineScope(Job(backgroundScope.coroutineContext.job))
        val second = CoroutineScope(Job(backgroundScope.coroutineContext.job))
        first.cancel()
        advanceUntilIdle()

        val linked = createLinkedScope(first, second, dispatcher)

        assertFalse(
            actual = linked.coroutineContext.job.isActive,
            message = "Linked scope should be cancelled on creation if a parent is already cancelled"
        )
    }

    @Test
    fun GIVEN_first_scope_without_job_WHEN_linked_scope_created_THEN_throws() {
        val scopeWithoutJob = object : CoroutineScope {
            override val coroutineContext: CoroutineContext = EmptyCoroutineContext
        }
        val scopeWithJob = CoroutineScope(Job())
        try {
            assertFailsWith<IllegalArgumentException> {
                createLinkedScope(scopeWithoutJob, scopeWithJob)
            }
        } finally {
            scopeWithJob.cancel()
        }
    }

    @Test
    fun GIVEN_second_scope_without_job_WHEN_linked_scope_created_THEN_throws() {
        val scopeWithJob = CoroutineScope(Job())
        val scopeWithoutJob = object : CoroutineScope {
            override val coroutineContext: CoroutineContext = EmptyCoroutineContext
        }
        try {
            assertFailsWith<IllegalArgumentException> {
                createLinkedScope(scopeWithJob, scopeWithoutJob)
            }
        } finally {
            scopeWithJob.cancel()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GIVEN_dispatcher_WHEN_linked_scope_created_THEN_linked_scope_uses_it() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val first = CoroutineScope(Job(backgroundScope.coroutineContext.job))
        val second = CoroutineScope(Job(backgroundScope.coroutineContext.job))

        val linked = createLinkedScope(first, second, dispatcher)

        assertEquals(
            expected = dispatcher,
            actual = linked.coroutineContext[ContinuationInterceptor],
            message = "Linked scope should use the provided dispatcher"
        )
    }
}
