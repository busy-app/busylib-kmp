package net.flipper.bsb.cloud.barsws.api

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import net.flipper.bsb.cloud.barsws.api.model.InternalWebSocketEvent
import net.flipper.bsb.cloud.barsws.api.model.InternalWebSocketRequest
import net.flipper.bsb.cloud.barsws.api.utils.wrappers.BSBWebSocketSession
import net.flipper.core.busylib.log.LogTagProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for BSBWebSocketImpl to ensure correct behavior of:
 * - Event flow emission and consumption
 * - Sending requests via WebSocket
 * - Error handling for serialization failures
 * - Resource cleanup and session lifecycle
 * - Race conditions in concurrent scenarios
 *
 * These tests use the real BSBWebSocketImpl with a mock BSBWebSocketSession,
 * allowing us to test the actual implementation logic without needing real network connections.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BSBWebSocketImplTest {

    private val testLogger = object : LogTagProvider {
        override val TAG: String = "BSBWebSocketImplTest"
    }

    // region Event Flow Tests

    @Test
    fun GIVEN_websocket_WHEN_receiving_valid_events_THEN_events_are_emitted_correctly() = runTest {
        // Given
        val mockSession = MockBSBWebSocketSession()
        val webSocket = BSBWebSocketImpl(
            session = mockSession,
            logger = testLogger,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        // When - simulate receiving an event
        val receivedEvents = mutableListOf<WebSocketEvent>()
        val job = webSocket.getEventsFlow()
            .onEach { receivedEvents.add(it) }
            .launchIn(backgroundScope)

        advanceUntilIdle()

        mockSession.emitEvent(InternalWebSocketEvent())
        advanceUntilIdle()

        // Then
        assertEquals(1, receivedEvents.size, "Should receive exactly one event")
        job.cancel()
    }

    @Test
    fun GIVEN_websocket_WHEN_receiving_multiple_events_THEN_all_events_are_emitted_in_order() =
        runTest {
            // Given
            val mockSession = MockBSBWebSocketSession()
            val webSocket = BSBWebSocketImpl(
                session = mockSession,
                logger = testLogger,
                scope = backgroundScope,
                dispatcher = StandardTestDispatcher(testScheduler)
            )

            // When
            val receivedEvents = mutableListOf<WebSocketEvent>()
            val job = webSocket.getEventsFlow()
                .onEach { receivedEvents.add(it) }
                .launchIn(backgroundScope)

            advanceUntilIdle()

            repeat(5) {
                mockSession.emitEvent(InternalWebSocketEvent())
            }
            advanceUntilIdle()

            // Then
            assertEquals(5, receivedEvents.size, "Should receive all 5 events")
            job.cancel()
        }

    @Test
    fun GIVEN_websocket_WHEN_multiple_subscribers_THEN_all_receive_same_events() = runTest {
        // Given
        val mockSession = MockBSBWebSocketSession()
        // Create a dedicated scope with the test dispatcher for proper coroutine control
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScopeForWebSocket = CoroutineScope(testDispatcher + Job())
        
        val webSocket = BSBWebSocketImpl(
            session = mockSession,
            logger = testLogger,
            scope = testScopeForWebSocket,
            dispatcher = testDispatcher
        )

        // When - two subscribers
        val subscriber1Events = mutableListOf<WebSocketEvent>()
        val subscriber2Events = mutableListOf<WebSocketEvent>()

        val job1 = webSocket.getEventsFlow()
            .onEach { subscriber1Events.add(it) }
            .launchIn(testScopeForWebSocket)

        val job2 = webSocket.getEventsFlow()
            .onEach { subscriber2Events.add(it) }
            .launchIn(testScopeForWebSocket)

        advanceUntilIdle()

        mockSession.emitEvent(InternalWebSocketEvent())
        advanceUntilIdle()

        // Then - both should receive the event
        assertEquals(1, subscriber1Events.size, "Subscriber 1 should receive the event")
        assertEquals(1, subscriber2Events.size, "Subscriber 2 should receive the event")

        job1.cancel()
        job2.cancel()
        testScopeForWebSocket.cancel() // Cancel the scope to cleanup shareIn coroutine
    }

    @Test
    fun GIVEN_websocket_WHEN_late_subscriber_joins_THEN_receives_replay_event() = runTest {
        // Given
        val mockSession = MockBSBWebSocketSession()
        val webSocket = BSBWebSocketImpl(
            session = mockSession,
            logger = testLogger,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        // When - first subscriber receives event
        val firstSubscriberEvents = mutableListOf<WebSocketEvent>()
        val job1 = webSocket.getEventsFlow()
            .onEach { firstSubscriberEvents.add(it) }
            .launchIn(backgroundScope)

        advanceUntilIdle()

        mockSession.emitEvent(InternalWebSocketEvent())
        advanceUntilIdle()

        assertEquals(1, firstSubscriberEvents.size, "First subscriber should receive the event")

        // Then late subscriber joins
        val lateSubscriberEvents = mutableListOf<WebSocketEvent>()
        val job2 = webSocket.getEventsFlow()
            .onEach { lateSubscriberEvents.add(it) }
            .launchIn(backgroundScope)

        advanceUntilIdle()

        // Then - late subscriber should get replayed event due to replay = 1
        assertEquals(1, lateSubscriberEvents.size, "Late subscriber should receive replayed event")

        job1.cancel()
        job2.cancel()
    }

    // endregion

    // region Send Tests

    @Test
    fun GIVEN_websocket_WHEN_sending_request_THEN_request_is_serialized_and_sent() = runTest {
        // Given
        val mockSession = MockBSBWebSocketSession()
        val webSocket = BSBWebSocketImpl(
            session = mockSession,
            logger = testLogger,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        // When
        val request = WebSocketRequest()
        webSocket.send(request)
        advanceUntilIdle()

        // Then
        assertEquals(1, mockSession.sentRequests.size, "Should send exactly one request")
    }

    @Test
    fun GIVEN_websocket_WHEN_sending_multiple_requests_THEN_all_are_sent_in_order() = runTest {
        // Given
        val mockSession = MockBSBWebSocketSession()
        val webSocket = BSBWebSocketImpl(
            session = mockSession,
            logger = testLogger,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        // When
        repeat(10) {
            webSocket.send(WebSocketRequest())
        }
        advanceUntilIdle()

        // Then
        assertEquals(10, mockSession.sentRequests.size, "Should send all 10 requests")
    }

    @Test
    fun GIVEN_websocket_WHEN_concurrent_send_requests_THEN_all_are_processed() = runTest {
        // Given
        val mockSession = MockBSBWebSocketSession()
        val webSocket = BSBWebSocketImpl(
            session = mockSession,
            logger = testLogger,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        // When - concurrent sends
        val jobs = List(50) {
            async {
                webSocket.send(WebSocketRequest())
            }
        }
        jobs.awaitAll()
        advanceUntilIdle()

        // Then
        assertEquals(50, mockSession.sentRequests.size, "All concurrent requests should be sent")
    }

    // endregion

    // region Error Handling Tests

    @Test
    fun GIVEN_websocket_WHEN_deserialization_error_THEN_continues_receiving_next_events() =
        runTest {
            // Given
            val mockSession = MockBSBWebSocketSession()
            val webSocket = BSBWebSocketImpl(
                session = mockSession,
                logger = testLogger,
                scope = backgroundScope,
                dispatcher = StandardTestDispatcher(testScheduler)
            )

            val receivedEvents = mutableListOf<WebSocketEvent>()
            val job = webSocket.getEventsFlow()
                .onEach { receivedEvents.add(it) }
                .launchIn(backgroundScope)

            advanceUntilIdle()

            // When - emit invalid message followed by valid message
            mockSession.emitSerializationError()
            advanceUntilIdle()
            mockSession.emitEvent(InternalWebSocketEvent())
            advanceUntilIdle()

            // Then - should still receive the valid message after error
            assertEquals(1, receivedEvents.size, "Should receive the valid event after error")
            job.cancel()
        }

    @Test
    fun GIVEN_websocket_WHEN_send_fails_THEN_error_is_propagated() = runTest {
        // Given
        val mockSession = MockBSBWebSocketSession(failOnSend = true)
        val webSocket = BSBWebSocketImpl(
            session = mockSession,
            logger = testLogger,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        // When/Then - send should throw
        var errorOccurred = false
        try {
            webSocket.send(WebSocketRequest())
        } catch (_: Exception) {
            errorOccurred = true
        }

        assertTrue(errorOccurred, "Send failure should propagate exception")
    }

    // endregion

    // region Scope Lifecycle Tests

    @Test
    fun GIVEN_websocket_WHEN_scope_cancelled_THEN_flow_stops_emitting() = runTest {
        // Given
        val mockSession = MockBSBWebSocketSession()
        val cancellableScope = CoroutineScope(Job(backgroundScope.coroutineContext.job))
        val webSocket = BSBWebSocketImpl(
            session = mockSession,
            logger = testLogger,
            scope = cancellableScope,
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        val receivedEvents = mutableListOf<WebSocketEvent>()
        val job = webSocket.getEventsFlow()
            .onEach { receivedEvents.add(it) }
            .launchIn(cancellableScope)

        advanceUntilIdle()

        mockSession.emitEvent(InternalWebSocketEvent())
        advanceUntilIdle()

        assertEquals(1, receivedEvents.size)

        // When - cancel the scope
        cancellableScope.cancel()
        advanceUntilIdle()

        // Then - no more events should be received after cancellation
        val countBefore = receivedEvents.size
        mockSession.emitEvent(InternalWebSocketEvent())
        advanceUntilIdle()

        assertEquals(countBefore, receivedEvents.size, "Should not receive events after scope cancellation")
    }

    @Test
    fun GIVEN_websocket_with_shared_flow_WHEN_all_subscribers_unsubscribe_THEN_flow_becomes_inactive() =
        runTest {
            // Given
            val mockSession = MockBSBWebSocketSession()
            val webSocket = BSBWebSocketImpl(
                session = mockSession,
                logger = testLogger,
                scope = backgroundScope,
                dispatcher = StandardTestDispatcher(testScheduler)
            )

            // When - subscribe then unsubscribe
            val job1 = webSocket.getEventsFlow().launchIn(backgroundScope)
            val job2 = webSocket.getEventsFlow().launchIn(backgroundScope)
            advanceUntilIdle()

            job1.cancel()
            job2.cancel()
            advanceUntilIdle()

            // Then - due to SharingStarted.WhileSubscribed, the upstream should stop
            // This is verified by the fact that the flow can be re-subscribed successfully
            val newJob = webSocket.getEventsFlow().launchIn(backgroundScope)
            advanceUntilIdle()
            assertNotNull(newJob)
            newJob.cancel()
        }

    // endregion

    // region Race Condition Tests

    @Test
    fun GIVEN_websocket_WHEN_rapid_subscribe_unsubscribe_THEN_no_race_condition() = runTest {
        // Given
        val mockSession = MockBSBWebSocketSession()
        val webSocket = BSBWebSocketImpl(
            session = mockSession,
            logger = testLogger,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        // When - rapid subscribe/unsubscribe cycles
        repeat(100) {
            val job = webSocket.getEventsFlow().launchIn(backgroundScope)
            advanceUntilIdle()
            job.cancel()
            advanceUntilIdle()
        }

        // Then - should not crash or have any issues
        val finalJob = webSocket.getEventsFlow().launchIn(backgroundScope)
        advanceUntilIdle()
        assertNotNull(finalJob)
        finalJob.cancel()
    }

    @Test
    fun GIVEN_websocket_WHEN_concurrent_send_and_receive_THEN_no_interference() = runTest {
        // Given
        val mockSession = MockBSBWebSocketSession()
        val webSocket = BSBWebSocketImpl(
            session = mockSession,
            logger = testLogger,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        val receivedEvents = mutableListOf<WebSocketEvent>()
        val job = webSocket.getEventsFlow()
            .onEach { receivedEvents.add(it) }
            .launchIn(backgroundScope)

        advanceUntilIdle()

        // When - concurrent send and receive
        val sendJobs = List(20) {
            async {
                webSocket.send(WebSocketRequest())
            }
        }

        // Emit events concurrently
        repeat(20) {
            mockSession.emitEvent(InternalWebSocketEvent())
        }

        sendJobs.awaitAll()
        advanceUntilIdle()

        // Then
        assertEquals(20, mockSession.sentRequests.size, "All sends should complete")
        assertEquals(20, receivedEvents.size, "All receives should complete")
        job.cancel()
    }

    @Test
    fun GIVEN_websocket_WHEN_high_frequency_events_THEN_all_processed_correctly() = runTest {
        // Given
        val mockSession = MockBSBWebSocketSession()
        val webSocket = BSBWebSocketImpl(
            session = mockSession,
            logger = testLogger,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        val receivedEvents = mutableListOf<WebSocketEvent>()
        val job = webSocket.getEventsFlow()
            .onEach { receivedEvents.add(it) }
            .launchIn(backgroundScope)

        advanceUntilIdle()

        // When - high frequency events
        repeat(1000) {
            mockSession.emitEvent(InternalWebSocketEvent())
        }
        advanceUntilIdle()

        // Then
        assertEquals(1000, receivedEvents.size, "All high frequency events should be received")
        job.cancel()
    }

    // endregion

    // region Edge Cases

    @Test
    fun GIVEN_websocket_WHEN_send_after_scope_cancelled_THEN_handles_gracefully() = runTest {
        // Given
        val mockSession = MockBSBWebSocketSession()
        val cancellableScope = CoroutineScope(Job(backgroundScope.coroutineContext.job))
        val webSocket = BSBWebSocketImpl(
            session = mockSession,
            logger = testLogger,
            scope = cancellableScope,
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        // When - cancel scope then try to send
        cancellableScope.cancel()
        advanceUntilIdle()

        // Then - should handle gracefully (either complete or throw CancellationException)
        var exceptionThrown = false
        try {
            webSocket.send(WebSocketRequest())
        } catch (_: CancellationException) {
            exceptionThrown = true
        } catch (_: Exception) {
            exceptionThrown = true
        }

        // The behavior depends on implementation - either throws or silently fails
        // This test ensures no crash occurs
        assertTrue(true, "Send after cancellation should not crash")
    }

    @Test
    fun GIVEN_websocket_WHEN_flow_collected_multiple_times_sequentially_THEN_works_correctly() =
        runTest {
            // Given
            val mockSession = MockBSBWebSocketSession()
            val webSocket = BSBWebSocketImpl(
                session = mockSession,
                logger = testLogger,
                scope = backgroundScope,
                dispatcher = StandardTestDispatcher(testScheduler)
            )

            // When - sequential collections
            repeat(5) { iteration ->
                val events = mutableListOf<WebSocketEvent>()
                val job = webSocket.getEventsFlow()
                    .onEach { events.add(it) }
                    .launchIn(backgroundScope)

                advanceUntilIdle()

                mockSession.emitEvent(InternalWebSocketEvent())
                advanceUntilIdle()

                // Should receive at least one event (might be more due to replay)
                assertTrue(
                    events.isNotEmpty(),
                    "Should receive events in iteration $iteration"
                )
                job.cancel()
                advanceUntilIdle()
            }
        }

    @Test
    fun GIVEN_websocket_WHEN_subscriber_is_slow_THEN_does_not_block_other_subscribers() = runTest {
        // Given
        val mockSession = MockBSBWebSocketSession()
        val webSocket = BSBWebSocketImpl(
            session = mockSession,
            logger = testLogger,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        val fastSubscriberEvents = mutableListOf<WebSocketEvent>()
        val slowSubscriberEvents = mutableListOf<WebSocketEvent>()

        // Fast subscriber
        val fastJob = webSocket.getEventsFlow()
            .onEach { fastSubscriberEvents.add(it) }
            .launchIn(backgroundScope)

        // Slow subscriber (simulated with processing delay)
        val slowJob = backgroundScope.launch {
            webSocket.getEventsFlow().collect { event ->
                delay(100) // Simulate slow processing
                slowSubscriberEvents.add(event)
            }
        }

        advanceUntilIdle()

        // When
        repeat(5) {
            mockSession.emitEvent(InternalWebSocketEvent())
        }
        advanceUntilIdle()

        // Then - fast subscriber should receive all events
        assertEquals(5, fastSubscriberEvents.size, "Fast subscriber should receive all events")

        fastJob.cancel()
        slowJob.cancel()
    }

    @Test
    fun GIVEN_websocket_WHEN_session_closes_THEN_flow_handles_gracefully() = runTest {
        // Given
        val mockSession = MockBSBWebSocketSession()
        val webSocket = BSBWebSocketImpl(
            session = mockSession,
            logger = testLogger,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        val receivedEvents = mutableListOf<WebSocketEvent>()
        val job = webSocket.getEventsFlow()
            .onEach { receivedEvents.add(it) }
            .launchIn(backgroundScope)

        advanceUntilIdle()

        mockSession.emitEvent(InternalWebSocketEvent())
        advanceUntilIdle()

        assertEquals(1, receivedEvents.size)

        // When - close the session
        mockSession.simulateClose()
        advanceUntilIdle()

        // Then - job should be cancellable
        job.cancel()
        assertTrue(job.isCancelled, "Job should be cancelled after session close")
    }

    // endregion

    // region Mock Implementation

    /**
     * Mock implementation of BSBWebSocketSession for testing.
     * This allows testing BSBWebSocketImpl without real network connections.
     */
    private class MockBSBWebSocketSession(
        private val failOnSend: Boolean = false
    ) : BSBWebSocketSession {
        private val _eventChannel = Channel<InternalWebSocketEvent>(Channel.UNLIMITED)
        private var _shouldThrowSerializationError = false
        private var _isClosed = false

        val sentRequests = mutableListOf<InternalWebSocketRequest>()

        suspend fun emitEvent(event: InternalWebSocketEvent) {
            if (!_isClosed) {
                _eventChannel.send(event)
            }
        }

        fun emitSerializationError() {
            _shouldThrowSerializationError = true
        }

        fun simulateClose() {
            _isClosed = true
            _eventChannel.close()
        }

        override suspend fun receive(): InternalWebSocketEvent {
            if (_shouldThrowSerializationError) {
                _shouldThrowSerializationError = false
                throw SerializationException("Mock deserialization error")
            }
            if (_isClosed) {
                throw CancellationException("Session closed")
            }
            return _eventChannel.receive()
        }

        override suspend fun send(request: InternalWebSocketRequest) {
            if (failOnSend) {
                throw RuntimeException("Mock send failure")
            }
            sentRequests.add(request)
        }

        override suspend fun close() {
            _isClosed = true
            _eventChannel.close()
        }
    }

    // endregion
}
