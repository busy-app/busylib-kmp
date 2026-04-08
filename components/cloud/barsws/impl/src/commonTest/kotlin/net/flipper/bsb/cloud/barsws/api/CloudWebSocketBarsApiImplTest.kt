package net.flipper.bsb.cloud.barsws.api

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.flipper.bsb.cloud.barsws.api.model.InternalWebSocketRequest
import net.flipper.bsb.cloud.barsws.api.model.WebSocketEvent
import net.flipper.bsb.cloud.barsws.api.utils.BSBWebSocket
import net.flipper.bsb.cloud.barsws.api.utils.CloudWebSocketApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class CloudWebSocketBarsApiImplTest {

    private val barId1 = Uuid.parse("11111111-1111-1111-1111-111111111111")
    private val barId2 = Uuid.parse("22222222-2222-2222-2222-222222222222")

    // region Subscribe/Unsubscribe Lifecycle

    @Test
    fun GIVEN_single_subscriber_WHEN_collecting_THEN_sends_subscribe() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()

            val job = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)

            assertEquals(
                listOf(InternalWebSocketRequest.SubscribeState(listOf(barId1))),
                env.mockWs.sentRequests
            )

            job.cancel()
        }

    @Test
    fun GIVEN_single_subscriber_WHEN_cancelled_THEN_sends_unsubscribe() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()

            val job = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)

            job.cancel()
            advanceUntilIdle()

            assertEquals(
                listOf(
                    InternalWebSocketRequest.SubscribeState(listOf(barId1)),
                    InternalWebSocketRequest.UnsubscribeState(listOf(barId1))
                ),
                env.mockWs.sentRequests
            )
        }

    @Test
    fun GIVEN_two_subscribers_same_bar_WHEN_both_active_THEN_sends_one_subscribe() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()

            val job1 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            val job2 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)

            val subscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.SubscribeState>()
            assertEquals(1, subscribes.size)

            job1.cancel()
            job2.cancel()
        }

    @Test
    fun GIVEN_two_subscribers_same_bar_WHEN_first_cancels_THEN_no_unsubscribe() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()

            val job1 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            val job2 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)

            job1.cancel()
            advanceUntilIdle()

            val unsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(0, unsubscribes.size)

            job2.cancel()
        }

    @Test
    fun GIVEN_two_subscribers_same_bar_WHEN_both_cancel_THEN_sends_one_unsubscribe() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()

            val job1 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            val job2 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)

            job1.cancel()
            advanceUntilIdle()
            job2.cancel()
            advanceUntilIdle()

            val unsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(1, unsubscribes.size)
        }

    @Test
    fun GIVEN_subscribers_to_different_bars_WHEN_active_THEN_each_gets_own_subscribe() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()

            val job1 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            val job2 = env.orchestrator.getEventsFlow(barId2)
                .launchIn(backgroundScope)

            val subscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.SubscribeState>()
            assertEquals(2, subscribes.size)
            assertTrue(subscribes.any { barId1 in it.idsToSubscribe })
            assertTrue(subscribes.any { barId2 in it.idsToSubscribe })

            job1.cancel()
            job2.cancel()
        }

    @Test
    fun GIVEN_subscribers_to_different_bars_WHEN_one_cancels_THEN_only_that_bar_unsubscribes() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()

            val job1 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            val job2 = env.orchestrator.getEventsFlow(barId2)
                .launchIn(backgroundScope)

            job1.cancel()
            advanceUntilIdle()

            val unsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(1, unsubscribes.size)
            assertTrue(barId1 in unsubscribes[0].idsToUnsubscribe)

            job2.cancel()
            advanceUntilIdle()

            val finalUnsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(2, finalUnsubscribes.size)
        }

    @Test
    fun GIVEN_all_cancelled_WHEN_resubscribing_THEN_sends_subscribe_again() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()

            // First cycle
            val job1 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            job1.cancel()
            advanceUntilIdle()

            // Second cycle
            val job2 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            job2.cancel()
            advanceUntilIdle()

            val subscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.SubscribeState>()
            val unsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(2, subscribes.size)
            assertEquals(2, unsubscribes.size)
        }

    // endregion

    // region Event Routing

    @Test
    fun GIVEN_event_for_subscribed_bar_WHEN_emitted_THEN_received_by_collector() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()
            val received = mutableListOf<Pair<String, Any>>()

            val job = env.orchestrator.getEventsFlow(barId1)
                .onEach { received.add(it) }
                .launchIn(backgroundScope)

            env.mockWs.emitEvent(WebSocketEvent(barId1, mapOf("key" to "value")))

            assertEquals(1, received.size)
            assertEquals("key" to ("value" as Any), received[0])

            job.cancel()
        }

    @Test
    fun GIVEN_event_for_other_bar_WHEN_emitted_THEN_not_received() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()
            val received = mutableListOf<Pair<String, Any>>()

            val job = env.orchestrator.getEventsFlow(barId1)
                .onEach { received.add(it) }
                .launchIn(backgroundScope)

            env.mockWs.emitEvent(WebSocketEvent(barId2, mapOf("key" to "value")))

            assertTrue(received.isEmpty())

            job.cancel()
        }

    @Test
    fun GIVEN_event_with_multiple_values_WHEN_emitted_THEN_flattened_to_pairs() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()
            val received = mutableListOf<Pair<String, Any>>()

            val job = env.orchestrator.getEventsFlow(barId1)
                .onEach { received.add(it) }
                .launchIn(backgroundScope)

            env.mockWs.emitEvent(
                WebSocketEvent(barId1, mapOf("a" to 1, "b" to "two", "c" to true))
            )

            assertEquals(3, received.size)
            assertTrue(received.contains("a" to (1 as Any)))
            assertTrue(received.contains("b" to ("two" as Any)))
            assertTrue(received.contains("c" to (true as Any)))

            job.cancel()
        }

    @Test
    fun GIVEN_multiple_events_WHEN_emitted_sequentially_THEN_all_received_in_order() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()
            val received = mutableListOf<Pair<String, Any>>()

            val job = env.orchestrator.getEventsFlow(barId1)
                .onEach { received.add(it) }
                .launchIn(backgroundScope)

            env.mockWs.emitEvent(WebSocketEvent(barId1, mapOf("first" to 1)))
            env.mockWs.emitEvent(WebSocketEvent(barId1, mapOf("second" to 2)))

            assertEquals(
                listOf("first" to (1 as Any), "second" to (2 as Any)),
                received
            )

            job.cancel()
        }

    @Test
    fun GIVEN_event_with_empty_values_WHEN_emitted_THEN_no_pairs_emitted() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()
            val received = mutableListOf<Pair<String, Any>>()

            val job = env.orchestrator.getEventsFlow(barId1)
                .onEach { received.add(it) }
                .launchIn(backgroundScope)

            env.mockWs.emitEvent(WebSocketEvent(barId1, emptyMap()))

            assertTrue(received.isEmpty())

            job.cancel()
        }

    @Test
    fun GIVEN_two_subscribers_same_bar_WHEN_event_emitted_THEN_both_receive_it() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()
            val received1 = mutableListOf<Pair<String, Any>>()
            val received2 = mutableListOf<Pair<String, Any>>()

            val job1 = env.orchestrator.getEventsFlow(barId1)
                .onEach { received1.add(it) }
                .launchIn(backgroundScope)
            val job2 = env.orchestrator.getEventsFlow(barId1)
                .onEach { received2.add(it) }
                .launchIn(backgroundScope)

            env.mockWs.emitEvent(WebSocketEvent(barId1, mapOf("key" to "value")))

            assertEquals(listOf("key" to ("value" as Any)), received1)
            assertEquals(listOf("key" to ("value" as Any)), received2)

            job1.cancel()
            job2.cancel()
        }

    @Test
    fun GIVEN_mixed_events_WHEN_emitted_THEN_each_subscriber_gets_only_own_bar() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()
            val received1 = mutableListOf<Pair<String, Any>>()
            val received2 = mutableListOf<Pair<String, Any>>()

            val job1 = env.orchestrator.getEventsFlow(barId1)
                .onEach { received1.add(it) }
                .launchIn(backgroundScope)
            val job2 = env.orchestrator.getEventsFlow(barId2)
                .onEach { received2.add(it) }
                .launchIn(backgroundScope)

            env.mockWs.emitEvent(WebSocketEvent(barId1, mapOf("bar1" to "a")))
            env.mockWs.emitEvent(WebSocketEvent(barId2, mapOf("bar2" to "b")))
            env.mockWs.emitEvent(WebSocketEvent(barId1, mapOf("bar1again" to "c")))

            assertEquals(
                listOf("bar1" to ("a" as Any), "bar1again" to ("c" as Any)),
                received1
            )
            assertEquals(
                listOf("bar2" to ("b" as Any)),
                received2
            )

            job1.cancel()
            job2.cancel()
        }

    // endregion

    // region Concurrency

    @Test
    fun GIVEN_many_concurrent_subscribers_same_bar_WHEN_active_THEN_one_subscribe_one_unsubscribe() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()

            val jobs = List(20) {
                env.orchestrator.getEventsFlow(barId1)
                    .launchIn(backgroundScope)
            }

            val subscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.SubscribeState>()
            assertEquals(1, subscribes.size)

            jobs.forEach { it.cancel() }
            advanceUntilIdle()

            val unsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(1, unsubscribes.size)
        }

    @Test
    fun GIVEN_five_subscribers_WHEN_three_cancel_THEN_no_unsubscribe_yet() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()

            val jobs = List(5) {
                env.orchestrator.getEventsFlow(barId1)
                    .launchIn(backgroundScope)
            }

            // Cancel 3 of 5
            jobs.take(3).forEach { it.cancel() }
            advanceUntilIdle()

            val unsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(0, unsubscribes.size)

            // Cancel remaining 2
            jobs.drop(3).forEach { it.cancel() }
            advanceUntilIdle()

            val finalUnsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(1, finalUnsubscribes.size)
        }

    @Test
    fun GIVEN_rapid_subscribe_unsubscribe_cycles_WHEN_same_bar_THEN_balanced_subscribe_unsubscribe() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()

            repeat(50) {
                val job = env.orchestrator.getEventsFlow(barId1)
                    .launchIn(backgroundScope)
                job.cancel()
                advanceUntilIdle()
            }

            val subscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.SubscribeState>()
            val unsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(subscribes.size, unsubscribes.size)
            assertEquals(50, subscribes.size)
        }

    @Test
    fun GIVEN_interleaved_bars_WHEN_subscribing_unsubscribing_THEN_no_cross_contamination() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()

            val job1 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            val job2 = env.orchestrator.getEventsFlow(barId2)
                .launchIn(backgroundScope)

            job1.cancel()
            advanceUntilIdle()

            val unsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(1, unsubscribes.size)
            assertTrue(barId1 in unsubscribes[0].idsToUnsubscribe)

            val job3 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)

            val allSubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.SubscribeState>()
            assertEquals(3, allSubscribes.size)

            job2.cancel()
            job3.cancel()
        }

    // endregion

    // region WebSocket Reconnection

    @Test
    fun GIVEN_active_subscriber_WHEN_websocket_changes_THEN_resubscribes_on_new_websocket() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()

            val job = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)

            assertEquals(1, env.mockWs.sentRequests.size)

            val newWs = MockBSBWebSocket()
            env.mockApi.setWebSocket(newWs)
            advanceUntilIdle()

            val newSubscribes = newWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.SubscribeState>()
            assertEquals(1, newSubscribes.size)
            assertTrue(barId1 in newSubscribes[0].idsToSubscribe)

            job.cancel()
        }

    @Test
    fun GIVEN_multiple_subscribers_same_bar_WHEN_websocket_changes_THEN_only_one_subscribe_on_new_ws() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()

            val job1 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            val job2 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)

            val newWs = MockBSBWebSocket()
            env.mockApi.setWebSocket(newWs)
            advanceUntilIdle()

            val newSubscribes = newWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.SubscribeState>()
            assertEquals(1, newSubscribes.size)

            job1.cancel()
            job2.cancel()
        }

    @Test
    fun GIVEN_subscriber_WHEN_websocket_changes_THEN_events_from_new_ws_are_received() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()
            val received = mutableListOf<Pair<String, Any>>()

            val job = env.orchestrator.getEventsFlow(barId1)
                .onEach { received.add(it) }
                .launchIn(backgroundScope)

            env.mockWs.emitEvent(WebSocketEvent(barId1, mapOf("old" to 1)))
            assertEquals(1, received.size)

            val newWs = MockBSBWebSocket()
            env.mockApi.setWebSocket(newWs)
            advanceUntilIdle()

            newWs.emitEvent(WebSocketEvent(barId1, mapOf("new" to 2)))

            assertEquals(2, received.size)
            assertEquals("new" to (2 as Any), received[1])

            job.cancel()
        }

    // endregion

    // region Edge Cases

    @Test
    fun GIVEN_no_websocket_initially_WHEN_websocket_appears_THEN_subscribes() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(initiallyConnected = false)

            val job = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)

            assertTrue(env.mockWs.sentRequests.isEmpty())

            env.mockApi.setWebSocket(env.mockWs)
            advanceUntilIdle()

            val subscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.SubscribeState>()
            assertEquals(1, subscribes.size)

            job.cancel()
        }

    @Test
    fun GIVEN_websocket_becomes_null_WHEN_last_subscriber_cancels_THEN_no_crash() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()

            val job = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)

            assertEquals(1, env.mockWs.sentRequests.size)

            env.mockApi.setWebSocket(null)
            advanceUntilIdle()

            job.cancel()
            advanceUntilIdle()

            val unsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(0, unsubscribes.size)
        }

    @Test
    fun GIVEN_many_bars_subscribed_WHEN_all_cancel_THEN_each_gets_unsubscribe() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()

            val barIds = List(20) { i ->
                Uuid.parse(
                    "00000000-0000-0000-0000-${i.toString().padStart(12, '0')}"
                )
            }

            val jobs = barIds.map { barId ->
                env.orchestrator.getEventsFlow(barId)
                    .launchIn(backgroundScope)
            }

            val subscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.SubscribeState>()
            assertEquals(20, subscribes.size)

            jobs.forEach { it.cancel() }
            advanceUntilIdle()

            val unsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(20, unsubscribes.size)
        }

    @Test
    fun GIVEN_subscriber_WHEN_events_arrive_before_and_after_cancel_THEN_only_before_received() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment()
            val received = mutableListOf<Pair<String, Any>>()

            val job = env.orchestrator.getEventsFlow(barId1)
                .onEach { received.add(it) }
                .launchIn(backgroundScope)

            env.mockWs.emitEvent(WebSocketEvent(barId1, mapOf("before" to 1)))

            job.cancel()
            advanceUntilIdle()

            env.mockWs.emitEvent(WebSocketEvent(barId1, mapOf("after" to 2)))

            assertEquals(1, received.size)
            assertEquals("before" to (1 as Any), received[0])
        }

    // endregion

    // region Test Infrastructure

    private class MockBSBWebSocket : BSBWebSocket {
        private val _sentRequests = mutableListOf<InternalWebSocketRequest>()
        private val requestsMutex = Mutex()
        val sentRequests: List<InternalWebSocketRequest> get() = _sentRequests.toList()

        private val _eventsFlow = MutableSharedFlow<WebSocketEvent>(
            extraBufferCapacity = 64
        )

        override fun getEventsFlow(): Flow<WebSocketEvent> = _eventsFlow

        override suspend fun send(request: InternalWebSocketRequest) {
            requestsMutex.withLock {
                _sentRequests.add(request)
            }
        }

        suspend fun emitEvent(event: WebSocketEvent) {
            _eventsFlow.emit(event)
        }
    }

    private class MockCloudWebSocketApi(
        initialWs: BSBWebSocket? = null
    ) : CloudWebSocketApi {
        private val _wsFlow = MutableStateFlow<BSBWebSocket?>(initialWs)

        override fun getWSFlow(): Flow<BSBWebSocket?> = _wsFlow

        fun setWebSocket(ws: BSBWebSocket?) {
            _wsFlow.value = ws
        }
    }

    private class TestEnvironment(initiallyConnected: Boolean = true) {
        val mockWs = MockBSBWebSocket()
        val mockApi = MockCloudWebSocketApi(
            initialWs = if (initiallyConnected) mockWs else null
        )
        val orchestrator = CloudWebSocketBarsApiImpl(mockApi)
    }

    // endregion
}
