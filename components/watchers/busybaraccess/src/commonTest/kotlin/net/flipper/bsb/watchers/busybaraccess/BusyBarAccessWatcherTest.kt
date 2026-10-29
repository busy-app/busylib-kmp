package net.flipper.bsb.watchers.busybaraccess

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.flipper.bsb.cloud.barsws.api.BSBWebSocket
import net.flipper.bsb.cloud.barsws.api.BUSYBarWebSocket
import net.flipper.bsb.cloud.barsws.api.CloudWebSocketApi
import net.flipper.bsb.cloud.barsws.api.WebSocketEvent
import net.flipper.eventbus.internal.BusyLibEventPublisher
import net.flipper.eventbus.model.BusyLibEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class BusyBarAccessWatcherTest {

    @Test
    fun GIVEN_websocket_WHEN_access_event_arrives_THEN_it_is_published_with_fields() = runTest {
        val webSocket = FakeWebSocket()
        val setup = createSetup(webSocket)
        val graceUntil = Instant.parse("2026-10-22T10:00:00Z")

        setup.watcher.onLaunch()
        advanceUntilIdle()
        webSocket.events.emit(WebSocketEvent.BusyBarAccessUpdatedEvent(granted = true, graceUntil = graceUntil))
        advanceUntilIdle()

        assertEquals(
            listOf<BusyLibEvent>(BusyLibEvent.BusyBarAccessUpdated(granted = true, graceUntil = graceUntil)),
            setup.publisher.events
        )
    }

    @Test
    fun GIVEN_websocket_WHEN_device_events_arrive_THEN_nothing_is_published() = runTest {
        val webSocket = FakeWebSocket()
        val setup = createSetup(webSocket)
        val device = BUSYBarWebSocket(cloudId = Uuid.random(), hardwareId = "hw", name = "Bar")

        setup.watcher.onLaunch()
        advanceUntilIdle()
        webSocket.events.emit(WebSocketEvent.LinkEvent(device))
        webSocket.events.emit(WebSocketEvent.UnlinkEvent(device))
        webSocket.events.emit(WebSocketEvent.NameChangeEvent(device))
        advanceUntilIdle()

        assertEquals(emptyList(), setup.publisher.events)
    }

    @Test
    fun GIVEN_no_websocket_WHEN_websocket_connects_THEN_its_access_events_are_published() = runTest {
        val setup = createSetup(webSocket = null)
        val webSocket = FakeWebSocket()

        setup.watcher.onLaunch()
        advanceUntilIdle()
        setup.webSocketFlow.value = webSocket
        advanceUntilIdle()
        webSocket.events.emit(WebSocketEvent.BusyBarAccessUpdatedEvent(granted = false, graceUntil = null))
        advanceUntilIdle()

        assertEquals(
            listOf<BusyLibEvent>(BusyLibEvent.BusyBarAccessUpdated(granted = false, graceUntil = null)),
            setup.publisher.events
        )
    }

    @Test
    fun GIVEN_reconnect_WHEN_old_and_new_websocket_emit_THEN_only_new_websocket_events_are_published() =
        runTest {
            val oldWebSocket = FakeWebSocket()
            val newWebSocket = FakeWebSocket()
            val setup = createSetup(oldWebSocket)

            setup.watcher.onLaunch()
            advanceUntilIdle()
            setup.webSocketFlow.value = newWebSocket
            advanceUntilIdle()
            oldWebSocket.events.emit(WebSocketEvent.BusyBarAccessUpdatedEvent(granted = true, graceUntil = null))
            newWebSocket.events.emit(WebSocketEvent.BusyBarAccessUpdatedEvent(granted = false, graceUntil = null))
            advanceUntilIdle()

            assertEquals(0, oldWebSocket.events.subscriptionCount.value)
            assertEquals(
                listOf<BusyLibEvent>(BusyLibEvent.BusyBarAccessUpdated(granted = false, graceUntil = null)),
                setup.publisher.events
            )
        }

    @Test
    fun GIVEN_websocket_lost_WHEN_old_websocket_emits_THEN_nothing_is_published() = runTest {
        val webSocket = FakeWebSocket()
        val setup = createSetup(webSocket)

        setup.watcher.onLaunch()
        advanceUntilIdle()
        setup.webSocketFlow.value = null
        advanceUntilIdle()
        webSocket.events.emit(WebSocketEvent.BusyBarAccessUpdatedEvent(granted = true, graceUntil = null))
        advanceUntilIdle()

        assertEquals(emptyList(), setup.publisher.events)
    }

    @Test
    fun GIVEN_launched_twice_WHEN_access_event_arrives_THEN_it_is_published_once() = runTest {
        val webSocket = FakeWebSocket()
        val setup = createSetup(webSocket)

        setup.watcher.onLaunch()
        setup.watcher.onLaunch()
        advanceUntilIdle()
        webSocket.events.emit(WebSocketEvent.BusyBarAccessUpdatedEvent(granted = true, graceUntil = null))
        advanceUntilIdle()

        assertEquals(1, webSocket.events.subscriptionCount.value)
        assertEquals(
            listOf<BusyLibEvent>(BusyLibEvent.BusyBarAccessUpdated(granted = true, graceUntil = null)),
            setup.publisher.events
        )
    }

    private fun TestScope.createSetup(webSocket: BSBWebSocket?): Setup {
        val webSocketFlow = MutableStateFlow(webSocket)
        val publisher = RecordingEventPublisher()
        val scope = CoroutineScope(
            SupervisorJob(backgroundScope.coroutineContext.job) + StandardTestDispatcher(testScheduler)
        )
        val watcher = BusyBarAccessWatcher(
            scope = scope,
            webSocketApi = FakeCloudWebSocketApi(webSocketFlow),
            eventPublisher = publisher
        )
        return Setup(watcher, webSocketFlow, publisher)
    }

    private class Setup(
        val watcher: BusyBarAccessWatcher,
        val webSocketFlow: MutableStateFlow<BSBWebSocket?>,
        val publisher: RecordingEventPublisher
    )

    private class FakeWebSocket : BSBWebSocket {
        val events = MutableSharedFlow<WebSocketEvent>()

        override fun getEventsFlow(): Flow<WebSocketEvent> = events
    }

    private class FakeCloudWebSocketApi(
        private val webSocketFlow: Flow<BSBWebSocket?>
    ) : CloudWebSocketApi {
        override fun getWSFlow(): Flow<BSBWebSocket?> = webSocketFlow
    }

    private class RecordingEventPublisher : BusyLibEventPublisher {
        val events: MutableList<BusyLibEvent> = mutableListOf()

        override suspend fun publish(event: BusyLibEvent) {
            events.add(event)
        }
    }
}
