package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class LaunchOnCompletionTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GIVEN_custom_scope_with_launchOnCompletion_WHEN_scope_is_cancelled_THEN_block_is_executed() {
        runTest(timeout = TIMEOUT) {
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
                    .first(),
                message = "Completion block should be executed after scope cancellation"
            )
            delay(3000L)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GIVEN_custom_scope_with_launchOnCompletion_WHEN_scope_suspends_and_cancelled_THEN_block_still_executes() {
        runTest(timeout = TIMEOUT) {
            val executionCountFlow = MutableStateFlow(0)
            val cancellationCountFlow = MutableStateFlow(0)

            CoroutineScope(Job(backgroundScope.coroutineContext.job)).let { cancellableScope ->

                cancellableScope.launch {
                    try {
                        delay(10000)
                    } catch (e: Exception) {
                        cancellationCountFlow.update { count -> count + 1 }
                        throw e
                    }
                }

                cancellableScope.launchOnCompletion {
                    executionCountFlow.update { count -> count + 1 }
                }

                delay(50)
                cancellableScope.cancel()
            }

            assertEquals(
                expected = 1,
                actual = cancellationCountFlow
                    .filter { count -> count == 1 }
                    .first(),
                message = "Job should be cancelled"
            )
            assertEquals(
                expected = 1,
                actual = executionCountFlow
                    .filter { count -> count == 1 }
                    .first(),
                message = "Completion block should be executed"
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GIVEN_custom_scope_with_multiple_launchOnCompletion_WHEN_scope_is_cancelled_THEN_all_blocks_execute() {
        runTest(timeout = TIMEOUT) {
            val executionCountFlow = MutableStateFlow(0)

            CoroutineScope(Job(backgroundScope.coroutineContext.job)).let { cancellableScope ->
                cancellableScope.launchOnCompletion {
                    executionCountFlow.update { count -> count + 1 }
                }

                cancellableScope.launchOnCompletion {
                    executionCountFlow.update { count -> count + 1 }
                }

                cancellableScope.launchOnCompletion {
                    executionCountFlow.update { count -> count + 1 }
                }
                cancellableScope.cancel()
            }

            assertEquals(
                expected = 3,
                actual = executionCountFlow
                    .filter { count -> count == 3 }
                    .first(),
                message = "All three completion blocks should be executed"
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GIVEN_custom_scope_with_launchOnCompletion_WHEN_block_throws_exception_THEN_exception_does_not_affect_parent() {
        runTest(timeout = TIMEOUT) {
            val executionFlagFlow = MutableStateFlow(false)

            CoroutineScope(Job(backgroundScope.coroutineContext.job)).let { cancellableScope ->
                cancellableScope.launchOnCompletion {
                    error("Test exception in completion block")
                }

                cancellableScope.launchOnCompletion {
                    executionFlagFlow.update { true }
                }

                cancellableScope.cancel()
            }

            assertTrue(
                actual = executionFlagFlow
                    .filter { isExecuted -> isExecuted }
                    .first(),
                message = "Second completion block should still execute"
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GIVEN_custom_scope_with_launchOnCompletion_WHEN_scope_is_cancelled_immediately_THEN_block_still_executes() {
        runTest(timeout = TIMEOUT) {
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
                    .first(),
                message = "Completion block should be executed even when cancelled immediately"
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun GIVEN_custom_scope_with_launchOnCompletion_WHEN_scope_completes_naturally_THEN_block_is_executed() {
        val executionFlagFlow = MutableStateFlow(false)
        runTest(timeout = TIMEOUT) {
            backgroundScope.launch {
                delay(50)
            }

            backgroundScope.launchOnCompletion {
                executionFlagFlow.update { true }
            }
        }
        runBlocking {
            assertTrue(
                actual = withTimeout(timeout = TIMEOUT) {
                    executionFlagFlow
                        .filter { isExecuted -> isExecuted }
                        .first()
                },
                message = "Completion block should be executed after natural scope completion"
            )
        }
    }

    companion object {
        private val TIMEOUT = 10.seconds
    }
}
