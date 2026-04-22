package net.flipper.bsb.cloud.barsws.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.flipper.bsb.cloud.barsws.api.BSBWebSocket
import net.flipper.bsb.cloud.barsws.api.ProtobufBase64
import net.flipper.bsb.cloud.barsws.api.WebSocketEvent
import net.flipper.bsb.cloud.barsws.api.model.InternalWebSocketRequest
import net.flipper.bsb.cloud.barsws.api.model.WebSocketEventInternal
import net.flipper.bsb.cloud.barsws.api.orchestrator.CloudWebSocketOrchestratorApiImpl
import net.flipper.bsb.cloud.barsws.api.utils.BSBWebSocketInternal
import net.flipper.bsb.cloud.barsws.api.utils.CloudWebSocketApiInternal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

private const val DEBOUNCE_ADVANCE_MS = 2000L

@OptIn(ExperimentalCoroutinesApi::class)
class CloudWebSocketOrchestratorApiImplTest {

    private val barId1 = Uuid.parse("11111111-1111-1111-1111-111111111111")
    private val barId2 = Uuid.parse("22222222-2222-2222-2222-222222222222")

    private fun TestScope.advanceDebounce() {
        testScheduler.advanceTimeBy(DEBOUNCE_ADVANCE_MS)
        testScheduler.runCurrent()
    }

    // region Subscribe/Unsubscribe Lifecycle

    @Test
    fun GIVEN_single_subscriber_WHEN_collecting_THEN_sends_subscribe() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(backgroundScope)

            val job = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            advanceDebounce()

            assertEquals(
                listOf(InternalWebSocketRequest.SubscribeState(listOf(barId1))),
                env.mockWs.sentRequests
            )

            job.cancel()
        }

    @Test
    fun GIVEN_single_subscriber_WHEN_cancelled_THEN_sends_unsubscribe() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(backgroundScope)

            val job = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            advanceDebounce()

            job.cancel()
            advanceDebounce()

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
            val env = TestEnvironment(backgroundScope)

            val job1 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            val job2 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            advanceDebounce()

            val subscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.SubscribeState>()
            assertEquals(1, subscribes.size)

            job1.cancel()
            job2.cancel()
        }

    @Test
    fun GIVEN_two_subscribers_same_bar_WHEN_first_cancels_THEN_no_unsubscribe() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(backgroundScope)

            val job1 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            val job2 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            advanceDebounce()

            job1.cancel()
            advanceDebounce()

            val unsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(0, unsubscribes.size)

            job2.cancel()
        }

    @Test
    fun GIVEN_two_subscribers_same_bar_WHEN_both_cancel_THEN_sends_one_unsubscribe() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(backgroundScope)

            val job1 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            val job2 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            advanceDebounce()

            job1.cancel()
            advanceDebounce()
            job2.cancel()
            advanceDebounce()

            val unsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(1, unsubscribes.size)
        }

    @Test
    fun GIVEN_subscribers_to_different_bars_WHEN_active_THEN_each_gets_own_subscribe() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(backgroundScope)

            val job1 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            val job2 = env.orchestrator.getEventsFlow(barId2)
                .launchIn(backgroundScope)
            advanceDebounce()

            val subscribedIds = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.SubscribeState>()
                .flatMap { it.idsToSubscribe }
                .toSet()
            assertEquals(setOf(barId1, barId2), subscribedIds)

            job1.cancel()
            job2.cancel()
        }

    @Test
    fun GIVEN_subscribers_to_different_bars_WHEN_one_cancels_THEN_only_that_bar_unsubscribes() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(backgroundScope)

            val job1 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            val job2 = env.orchestrator.getEventsFlow(barId2)
                .launchIn(backgroundScope)
            advanceDebounce()

            job1.cancel()
            advanceDebounce()

            val unsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(1, unsubscribes.size)
            assertTrue(barId1 in unsubscribes[0].idsToUnsubscribe)

            job2.cancel()
            advanceDebounce()

            val finalUnsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(2, finalUnsubscribes.size)
        }

    @Test
    fun GIVEN_all_cancelled_WHEN_resubscribing_THEN_sends_subscribe_again() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(backgroundScope)

            // First cycle
            val job1 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            advanceDebounce()
            job1.cancel()
            advanceDebounce()

            // Second cycle
            val job2 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            advanceDebounce()
            job2.cancel()
            advanceDebounce()

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
            val env = TestEnvironment(backgroundScope)
            val received = mutableListOf<ProtobufBase64>()

            val job = env.orchestrator.getEventsFlow(barId1)
                .onEach { received.add(it) }
                .launchIn(backgroundScope)
            advanceDebounce()

            env.mockWs.emitEvent(protobufEvent(barId1, "state1"))

            assertEquals(1, received.size)
            assertEquals(ProtobufBase64("state1"), received[0])

            job.cancel()
        }

    @Test
    fun GIVEN_event_for_other_bar_WHEN_emitted_THEN_not_received() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(backgroundScope)
            val received = mutableListOf<ProtobufBase64>()

            val job = env.orchestrator.getEventsFlow(barId1)
                .onEach { received.add(it) }
                .launchIn(backgroundScope)
            advanceDebounce()

            env.mockWs.emitEvent(protobufEvent(barId2, "state1"))

            assertTrue(received.isEmpty())

            job.cancel()
        }

    @Test
    fun GIVEN_multiple_events_WHEN_emitted_sequentially_THEN_all_received_in_order() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(backgroundScope)
            val received = mutableListOf<ProtobufBase64>()

            val job = env.orchestrator.getEventsFlow(barId1)
                .onEach { received.add(it) }
                .launchIn(backgroundScope)
            advanceDebounce()

            env.mockWs.emitEvent(protobufEvent(barId1, "first"))
            env.mockWs.emitEvent(protobufEvent(barId1, "second"))

            assertEquals(
                listOf(ProtobufBase64("first"), ProtobufBase64("second")),
                received
            )

            job.cancel()
        }

    @Test
    fun GIVEN_two_subscribers_same_bar_WHEN_event_emitted_THEN_both_receive_it() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(backgroundScope)
            val received1 = mutableListOf<ProtobufBase64>()
            val received2 = mutableListOf<ProtobufBase64>()

            val job1 = env.orchestrator.getEventsFlow(barId1)
                .onEach { received1.add(it) }
                .launchIn(backgroundScope)
            val job2 = env.orchestrator.getEventsFlow(barId1)
                .onEach { received2.add(it) }
                .launchIn(backgroundScope)
            advanceDebounce()

            env.mockWs.emitEvent(protobufEvent(barId1, "state1"))

            assertEquals(listOf(ProtobufBase64("state1")), received1)
            assertEquals(listOf(ProtobufBase64("state1")), received2)

            job1.cancel()
            job2.cancel()
        }

    @Test
    fun GIVEN_mixed_events_WHEN_emitted_THEN_each_subscriber_gets_only_own_bar() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(backgroundScope)
            val received1 = mutableListOf<ProtobufBase64>()
            val received2 = mutableListOf<ProtobufBase64>()

            val job1 = env.orchestrator.getEventsFlow(barId1)
                .onEach { received1.add(it) }
                .launchIn(backgroundScope)
            val job2 = env.orchestrator.getEventsFlow(barId2)
                .onEach { received2.add(it) }
                .launchIn(backgroundScope)
            advanceDebounce()

            env.mockWs.emitEvent(protobufEvent(barId1, "bar1-a"))
            env.mockWs.emitEvent(protobufEvent(barId2, "bar2-b"))
            env.mockWs.emitEvent(protobufEvent(barId1, "bar1-c"))

            assertEquals(
                listOf(ProtobufBase64("bar1-a"), ProtobufBase64("bar1-c")),
                received1
            )
            assertEquals(
                listOf(ProtobufBase64("bar2-b")),
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
            val env = TestEnvironment(backgroundScope)

            val jobs = List(20) {
                env.orchestrator.getEventsFlow(barId1)
                    .launchIn(backgroundScope)
            }
            advanceDebounce()

            val subscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.SubscribeState>()
            assertEquals(1, subscribes.size)

            jobs.forEach { it.cancel() }
            advanceDebounce()

            val unsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(1, unsubscribes.size)
        }

    @Test
    fun GIVEN_five_subscribers_WHEN_three_cancel_THEN_no_unsubscribe_yet() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(backgroundScope)

            val jobs = List(5) {
                env.orchestrator.getEventsFlow(barId1)
                    .launchIn(backgroundScope)
            }
            advanceDebounce()

            // Cancel 3 of 5
            jobs.take(3).forEach { it.cancel() }
            advanceDebounce()

            val unsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(0, unsubscribes.size)

            // Cancel remaining 2
            jobs.drop(3).forEach { it.cancel() }
            advanceDebounce()

            val finalUnsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(1, finalUnsubscribes.size)
        }

    @Test
    fun GIVEN_rapid_subscribe_unsubscribe_cycles_WHEN_same_bar_THEN_balanced_subscribe_unsubscribe() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(backgroundScope)

            repeat(50) {
                val job = env.orchestrator.getEventsFlow(barId1)
                    .launchIn(backgroundScope)
                advanceDebounce()
                job.cancel()
                advanceDebounce()
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
            val env = TestEnvironment(backgroundScope)

            val job1 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            val job2 = env.orchestrator.getEventsFlow(barId2)
                .launchIn(backgroundScope)
            advanceDebounce()

            job1.cancel()
            advanceDebounce()

            val unsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(1, unsubscribes.size)
            assertTrue(barId1 in unsubscribes[0].idsToUnsubscribe)

            val job3 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            advanceDebounce()

            val allSubscribedIds = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.SubscribeState>()
                .flatMap { it.idsToSubscribe }
            assertEquals(2, allSubscribedIds.count { it == barId1 })
            assertEquals(1, allSubscribedIds.count { it == barId2 })

            job2.cancel()
            job3.cancel()
        }

    // endregion

    // region WebSocket Reconnection

    @Test
    fun GIVEN_active_subscriber_WHEN_websocket_changes_THEN_resubscribes_on_new_websocket() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(backgroundScope)

            val job = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            advanceDebounce()

            assertEquals(1, env.mockWs.sentRequests.size)

            val newWs = MockBSBWebSocket()
            env.mockApi.setWebSocket(newWs)
            advanceDebounce()

            val newSubscribes = newWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.SubscribeState>()
            assertEquals(1, newSubscribes.size)
            assertTrue(barId1 in newSubscribes[0].idsToSubscribe)

            job.cancel()
        }

    @Test
    fun GIVEN_multiple_subscribers_same_bar_WHEN_websocket_changes_THEN_only_one_subscribe_on_new_ws() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(backgroundScope)

            val job1 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            val job2 = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            advanceDebounce()

            val newWs = MockBSBWebSocket()
            env.mockApi.setWebSocket(newWs)
            advanceDebounce()

            val newSubscribes = newWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.SubscribeState>()
            assertEquals(1, newSubscribes.size)

            job1.cancel()
            job2.cancel()
        }

    @Test
    fun GIVEN_subscriber_WHEN_websocket_changes_THEN_events_from_new_ws_are_received() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(backgroundScope)
            val received = mutableListOf<ProtobufBase64>()

            val job = env.orchestrator.getEventsFlow(barId1)
                .onEach { received.add(it) }
                .launchIn(backgroundScope)
            advanceDebounce()

            env.mockWs.emitEvent(protobufEvent(barId1, "old"))
            assertEquals(1, received.size)

            val newWs = MockBSBWebSocket()
            env.mockApi.setWebSocket(newWs)
            advanceDebounce()

            newWs.emitEvent(protobufEvent(barId1, "new"))

            assertEquals(2, received.size)
            assertEquals(ProtobufBase64("new"), received[1])

            job.cancel()
        }

    // endregion

    // region Edge Cases

    @Test
    fun GIVEN_no_websocket_initially_WHEN_websocket_appears_THEN_subscribes() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(backgroundScope, initiallyConnected = false)

            val job = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            advanceDebounce()

            assertTrue(env.mockWs.sentRequests.isEmpty())

            env.mockApi.setWebSocket(env.mockWs)
            advanceDebounce()

            val subscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.SubscribeState>()
            assertEquals(1, subscribes.size)

            job.cancel()
        }

    @Test
    fun GIVEN_websocket_becomes_null_WHEN_last_subscriber_cancels_THEN_no_crash() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(backgroundScope)

            val job = env.orchestrator.getEventsFlow(barId1)
                .launchIn(backgroundScope)
            advanceDebounce()

            assertEquals(1, env.mockWs.sentRequests.size)

            env.mockApi.setWebSocket(null)
            advanceDebounce()

            job.cancel()
            advanceDebounce()

            val unsubscribes = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
            assertEquals(1, unsubscribes.size)
        }

    @Test
    fun GIVEN_many_bars_subscribed_WHEN_all_cancel_THEN_each_gets_unsubscribe() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(backgroundScope)

            val barIds = List(20) { i ->
                Uuid.parse(
                    "00000000-0000-0000-0000-${i.toString().padStart(12, '0')}"
                )
            }

            val jobs = barIds.map { barId ->
                env.orchestrator.getEventsFlow(barId)
                    .launchIn(backgroundScope)
            }
            advanceDebounce()

            val subscribedIds = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.SubscribeState>()
                .flatMap { it.idsToSubscribe }
                .toSet()
            assertEquals(barIds.toSet(), subscribedIds)

            jobs.forEach { it.cancel() }
            advanceDebounce()

            val unsubscribedIds = env.mockWs.sentRequests
                .filterIsInstance<InternalWebSocketRequest.UnsubscribeState>()
                .flatMap { it.idsToUnsubscribe }
                .toSet()
            assertEquals(barIds.toSet(), unsubscribedIds)
        }

    @Test
    fun GIVEN_subscriber_WHEN_events_arrive_before_and_after_cancel_THEN_only_before_received() =
        runTest(UnconfinedTestDispatcher()) {
            val env = TestEnvironment(backgroundScope)
            val received = mutableListOf<ProtobufBase64>()

            val job = env.orchestrator.getEventsFlow(barId1)
                .onEach { received.add(it) }
                .launchIn(backgroundScope)
            advanceDebounce()

            env.mockWs.emitEvent(protobufEvent(barId1, "before"))

            job.cancel()
            advanceDebounce()

            env.mockWs.emitEvent(protobufEvent(barId1, "after"))

            assertEquals(1, received.size)
            assertEquals(ProtobufBase64("before"), received[0])
        }

    // endregion

    // region Test Infrastructure

    private fun protobufEvent(cloudId: Uuid, state: String) =
        WebSocketEventInternal.Protobuf(cloudId = cloudId, state = state)

    private class MockBSBWebSocket : BSBWebSocketInternal {
        private val _sentRequests = mutableListOf<InternalWebSocketRequest>()
        private val requestsMutex = Mutex()
        val sentRequests: List<InternalWebSocketRequest> get() = _sentRequests.toList()

        private val _eventsFlow = MutableSharedFlow<WebSocketEventInternal>(
            extraBufferCapacity = 64
        )

        override fun getEventsFlow(): Flow<WebSocketEvent> =
            kotlinx.coroutines.flow.emptyFlow()

        override fun getEventsFlowInternal(): Flow<WebSocketEventInternal> = _eventsFlow

        override suspend fun send(request: InternalWebSocketRequest) {
            requestsMutex.withLock {
                _sentRequests.add(request)
            }
        }

        suspend fun emitEvent(event: WebSocketEventInternal) {
            _eventsFlow.emit(event)
        }
    }

    private class MockCloudWebSocketApi(
        initialWs: BSBWebSocketInternal? = null
    ) : CloudWebSocketApiInternal {
        private val _wsFlow = MutableStateFlow<BSBWebSocketInternal?>(initialWs)

        override fun getWSFlow(): Flow<BSBWebSocket?> = _wsFlow

        override fun getWSInternalFlow(): Flow<BSBWebSocketInternal?> = _wsFlow

        fun setWebSocket(ws: BSBWebSocketInternal?) {
            _wsFlow.value = ws
        }
    }

    private class TestEnvironment(
        scope: CoroutineScope,
        initiallyConnected: Boolean = true
    ) {
        val mockWs = MockBSBWebSocket()
        val mockApi = MockCloudWebSocketApi(
            initialWs = if (initiallyConnected) mockWs else null
        )
        val orchestrator = CloudWebSocketOrchestratorApiImpl(mockApi, scope)
    }

    // endregion
}
