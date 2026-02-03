package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class TransformWhileSubscribedSharedFlowTest {

    private fun createFlow(): Flow<Int> {
        return flow {
            var i = 0
            while (currentCoroutineContext().isActive) {
                emit(i++)
                delay(1000L)
            }
        }
    }

    @Test
    fun GIVEN_no_subscribers_WHEN_flow_created_THEN_upstream_should_not_emit() {
        runTest {
            var emissionCount = 0
            flow {
                while (currentCoroutineContext().isActive) {
                    emissionCount++
                    emit(1)
                    delay(1000L)
                }
            }.transformWhileSubscribed(
                timeout = 5.seconds,
                scope = backgroundScope,
                transformFlow = { flow -> flow }
            )
            advanceTimeBy(10000)
            assertEquals(
                expected = 0,
                actual = emissionCount,
                message = "Upstream should not emit when there are no subscribers"
            )
        }
    }

    @Test
    fun GIVEN_no_subscribers_WHEN_first_subscriber_added_THEN_upstream_starts_emitting() {
        runTest {
            var transformationCount = 0
            val sharedFlow = createFlow().transformWhileSubscribed(
                timeout = 5.seconds,
                scope = backgroundScope,
                transformFlow = { flow -> flow }
            )

            async {
                sharedFlow.first()
                transformationCount++
            }.await()

            assertEquals(
                expected = 1,
                actual = transformationCount,
                message = "Upstream should start emitting when first subscriber is added"
            )
        }
    }

    @Test
    fun GIVEN_one_subscriber_WHEN_second_subscriber_added_THEN_both_receive_same_values() {
        runTest {
            val sharedFlow = createFlow().transformWhileSubscribed(
                timeout = 5.seconds,
                scope = backgroundScope,
                transformFlow = { flow -> flow }
            )

            val firstSubscriberValue = async {
                sharedFlow
                    .onStart { advanceTimeBy(1500) }
                    .drop(1)
                    .first()
            }.await()

            val secondSubscriberValue = async { sharedFlow.first() }.await()

            assertEquals(
                expected = 1,
                actual = firstSubscriberValue,
                message = "First subscriber should receive the first value"
            )

            assertEquals(
                expected = firstSubscriberValue,
                actual = secondSubscriberValue,
                message = "Both subscribers should receive the same values"
            )
        }
    }

    @Test
    fun GIVEN_two_subscribers_WHEN_one_subscriber_removed_THEN_remaining_subscriber_still_receives_values() {
        runTest {
            var emissionCount = 0
            val sharedFlow = createFlow().transformWhileSubscribed(
                timeout = 5.seconds,
                scope = backgroundScope,
                transformFlow = { flow -> flow }
            )
            val job1 = launch { sharedFlow.take(100).collect() }
            val job2 = launch { sharedFlow.collect { emissionCount++ } }

            advanceTimeBy(1500)
            job1.cancel()

            val valuesBefore = emissionCount
            advanceTimeBy(2000)
            val valuesAfter = emissionCount

            assertTrue(
                actual = valuesAfter > valuesBefore,
                message = "Remaining subscriber should continue receiving values"
            )
            job2.cancel()
        }
    }

    @Test
    fun GIVEN_one_subscriber_WHEN_subscriber_removed_THEN_transform_stops_during_grace_period() {
        runTest {
            var transformationCount = 0
            val sharedFlow = createFlow().transformWhileSubscribed(
                timeout = 5.seconds,
                scope = backgroundScope,
                transformFlow = { flow ->
                    transformationCount++
                    flow
                }
            )

            val job = launch { sharedFlow.take(100).collect() }

            advanceTimeBy(1500)
            val transformationCountWithSubscriber = transformationCount

            job.cancel()

            advanceTimeBy(2000)
            val transformationCountDuringGracePeriod = transformationCount

            assertEquals(
                expected = transformationCountWithSubscriber,
                actual = transformationCountDuringGracePeriod,
                message = "Transform should not be called during grace period without subscribers"
            )
        }
    }

    @Test
    fun GIVEN_no_subscribers_in_grace_period_WHEN_timeout_expires_THEN_upstream_stops() {
        runTest {
            var transformationCount = 0
            val sharedFlow = createFlow().transformWhileSubscribed(
                timeout = 5.seconds,
                scope = backgroundScope,
                transformFlow = { flow -> flow }
            )

            val job = launch { sharedFlow.take(100).collect() }

            advanceTimeBy(1500)
            job.cancel()

            val transformationCountBeforeTimeout = transformationCount

            advanceTimeBy(6000)

            val transformationCountAfterTimeout = transformationCount

            assertEquals(
                expected = transformationCountBeforeTimeout,
                actual = transformationCountAfterTimeout,
                message = "Upstream should stop after timeout expires"
            )
        }
    }

    @Test
    fun GIVEN_upstream_stopped_after_timeout_WHEN_new_subscriber_added_THEN_upstream_restarts() {
        runTest {
            val sharedFlow = createFlow().transformWhileSubscribed(
                timeout = 5.seconds,
                scope = backgroundScope,
                transformFlow = { flow -> flow }
            )

            launch { sharedFlow.take(100).collect() }
                .also { advanceTimeBy(1500) }
                .cancelAndJoin()
                .also { advanceTimeBy(6000) }

            val job2 = launch { sharedFlow.first() }

            assertEquals(
                expected = 0,
                actual = sharedFlow.first(),
                message = "Upstream should restart when new subscriber is added after timeout"
            )

            job2.cancel()
        }
    }

    @Test
    fun GIVEN_subscriber_active_WHEN_value_emitted_THEN_transformed_value_is_cached() {
        runTest {
            val sharedFlow = createFlow().transformWhileSubscribed(
                timeout = 5.seconds,
                scope = backgroundScope,
                transformFlow = { flow -> flow }
            )

            launch { sharedFlow.collect() }
                .also { advanceTimeBy(1500) }
                .also(Job::cancel)

            assertEquals(
                expected = 1,
                actual = sharedFlow.first(),
                message = "Latest value should be cached in replay cache"
            )
        }
    }

    @Test
    fun GIVEN_latest_subscriber_in_grace_period_WHEN_collects_THEN_values_equals() {
        runTest {
            var transformationCount = 0
            val sharedFlow = createFlow().transformWhileSubscribed(
                timeout = 500.seconds,
                scope = backgroundScope,
                transformFlow = { flow ->
                    flow.onEach {
                        transformationCount++
                    }.mapLatest { "mapped_$it" }
                }
            )

            val firstDeferred = async { sharedFlow.drop(2).first() }

            advanceTimeBy(2000)
            advanceTimeBy(30.seconds)
            firstDeferred.await()

            val secondDeferred = async { sharedFlow.drop(2).first() }
            secondDeferred.await()
            assertEquals(
                expected = "mapped_2",
                actual = firstDeferred.await()
            )
            assertEquals(
                expected = "mapped_33",
                actual = secondDeferred.await()
            )
            assertEquals(
                expected = 5,
                actual = transformationCount,
                message = "Transformation should be applied only from [0;2] and [6;7]"
            )
        }
    }
}
