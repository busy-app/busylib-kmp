package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import net.flipper.core.busylib.log.error
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class LaunchOnCompletionTest {
    private suspend fun <T> Flow<T>.timeoutFirst(
        timeout: Duration,
        message: () -> String
    ): T {
        return try {
            withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(timeout) {
                    first()
                }
            }
        } catch (t: TimeoutCancellationException) {
            error { message.invoke() }
            throw t
        }
    }

    private fun CoroutineScope.launchOnCompletion(
        mutex: Mutex,
        block: suspend () -> Unit
    ) = launchOnCompletion {
        mutex.withLock {
            block.invoke()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GIVEN_custom_scope_with_launchOnCompletion_WHEN_scope_is_cancelled_THEN_block_is_executed() {
        runTest {
            val executionFlagFlow = MutableStateFlow(false)
            CoroutineScope(Job(backgroundScope.coroutineContext.job)).let { cancellableScope ->
                cancellableScope.launchOnCompletion {
                    executionFlagFlow.update { true }
                }
                cancellableScope.cancel()
            }
            assertTrue(
                actual = executionFlagFlow
                    .filter { isExecuted -> isExecuted }
                    .timeoutFirst(TIMEOUT) { "Completion block should be executed after scope cancellation" },
                message = "Completion block should be executed after scope cancellation"
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GIVEN_custom_scope_with_launchOnCompletion_WHEN_scope_suspends_and_cancelled_THEN_block_still_executes() {
        runTest {
            val executionCountFlow = MutableStateFlow(0)
            val cancellationCountFlow = MutableStateFlow(0)

            CoroutineScope(Job(backgroundScope.coroutineContext.job)).let { cancellableScope ->
                cancellableScope.coroutineContext.job.invokeOnCompletion { t ->
                    if (t is CancellationException) {
                        cancellationCountFlow.update { count -> count + 1 }
                    }
                }
                cancellableScope.launchOnCompletion {
                    executionCountFlow.update { count -> count + 1 }
                }
                cancellableScope.cancel()
            }
            assertEquals(
                expected = 1,
                actual = cancellationCountFlow
                    .filter { count -> count == 1 }
                    .timeoutFirst(TIMEOUT) { "Cancellation count should be 1" },
                message = "Job should be cancelled"
            )
            assertEquals(
                expected = 1,
                actual = executionCountFlow
                    .filter { count -> count == 1 }
                    .timeoutFirst(TIMEOUT) { "Execution count should be 1" },
                message = "Completion block should be executed"
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GIVEN_custom_scope_with_multiple_launchOnCompletion_WHEN_scope_is_cancelled_THEN_all_blocks_execute() {
        val executionCountFlow = MutableStateFlow(0)
        runTest {
            val mutex = Mutex()

            CoroutineScope(Job(backgroundScope.coroutineContext.job)).let { cancellableScope ->
                cancellableScope.launchOnCompletion(mutex) {
                    executionCountFlow.update { count -> count + 1 }
                }

                cancellableScope.launchOnCompletion(mutex) {
                    executionCountFlow.update { count -> count + 1 }
                }

                cancellableScope.launchOnCompletion(mutex) {
                    executionCountFlow.update { count -> count + 1 }
                }
                cancellableScope.cancel()
            }
            assertEquals(
                expected = 3,
                actual = executionCountFlow
                    .filter { count -> count == 3 }
                    .timeoutFirst(
                        TIMEOUT
                    ) { "All three completion blocks should be executed. was ${executionCountFlow.value}" },
                message = "All three completion blocks should be executed"
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GIVEN_custom_scope_with_launchOnCompletion_WHEN_block_throws_exception_THEN_exception_does_not_affect_parent() {
        runTest {
            val mutex = Mutex()
            val executionFlagFlow = MutableStateFlow(false)

            CoroutineScope(Job(backgroundScope.coroutineContext.job)).let { cancellableScope ->
                cancellableScope.launchOnCompletion(mutex) {
                    error("Test exception in completion block")
                }

                cancellableScope.launchOnCompletion(mutex) {
                    executionFlagFlow.update { true }
                }
                cancellableScope.cancel()
            }

            assertTrue(
                actual = executionFlagFlow
                    .filter { isExecuted -> isExecuted }
                    .timeoutFirst(TIMEOUT) { "Second completion block should still execute" },
                message = "Second completion block should still execute"
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GIVEN_custom_scope_with_launchOnCompletion_WHEN_scope_is_cancelled_immediately_THEN_block_still_executes() {
        runTest {
            val executionFlagFlow = MutableStateFlow(false)

            CoroutineScope(backgroundScope.coroutineContext).let { cancellableScope ->

                cancellableScope.launchOnCompletion {
                    executionFlagFlow.update { true }
                }
                cancellableScope.cancel()
            }

            assertTrue(
                actual = executionFlagFlow
                    .filter { isExecuted -> isExecuted }
                    .timeoutFirst(TIMEOUT) { "Completion block should be executed even when cancelled immediately" },
                message = "Completion block should be executed even when cancelled immediately"
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GIVEN_custom_scope_with_launchOnCompletion_WHEN_scope_completes_naturally_THEN_block_is_executed() {
        val executionFlagFlow = MutableStateFlow(false)
        runTest {
            backgroundScope.launchOnCompletion {
                executionFlagFlow.update { true }
            }
        }
        runBlocking {
            assertTrue(
                actual = executionFlagFlow
                    .filter { isExecuted -> isExecuted }
                    .timeoutFirst(TIMEOUT) { "Completion block should be executed after natural scope completion" },
                message = "Completion block should be executed after natural scope completion"
            )
        }
    }

    companion object {
        private val TIMEOUT = 2.seconds
    }
}
