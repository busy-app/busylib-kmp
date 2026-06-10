package net.flipper.eventbus.impl

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.eventbus.model.BusyLibEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class EventBusApiImplTest {

    private fun device(uniqueId: String): BUSYBar = BUSYBar(
        humanReadableName = "Device $uniqueId",
        uniqueId = uniqueId,
        cloud = BUSYBar.ConnectionWay.Cloud(Uuid.random())
    )

    private fun event(uniqueId: String): BusyLibEvent =
        BusyLibEvent.ActiveDeviceAutoSwitched(device(uniqueId))

    @Test
    fun subscriberReceivesEventPublishedAfterSubscribing() = runTest {
        val eventBus = EventBusApiImpl()
        val received = mutableListOf<BusyLibEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            eventBus.getEvents().collect(received::add)
        }

        val published = event("a")
        eventBus.publish(published)

        assertEquals(listOf(published), received)
    }

    @Test
    fun everyActiveSubscriberReceivesTheSameEvent() = runTest {
        val eventBus = EventBusApiImpl()
        val firstReceived = mutableListOf<BusyLibEvent>()
        val secondReceived = mutableListOf<BusyLibEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            eventBus.getEvents().collect(firstReceived::add)
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            eventBus.getEvents().collect(secondReceived::add)
        }

        val published = event("a")
        eventBus.publish(published)

        assertEquals(listOf(published), firstReceived)
        assertEquals(listOf(published), secondReceived)
    }

    @Test
    fun eventPublishedWhileNobodySubscribesIsDropped() = runTest {
        val eventBus = EventBusApiImpl()

        // No subscriber yet: this event must be dropped, not buffered for replay.
        eventBus.publish(event("dropped"))

        val received = mutableListOf<BusyLibEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            eventBus.getEvents().collect(received::add)
        }
        val afterSubscribe = event("delivered")
        eventBus.publish(afterSubscribe)

        assertEquals(listOf(afterSubscribe), received)
    }

    @Test
    fun concurrentPublishesAreAllDeliveredToActiveSubscriber() = runTest {
        val eventBus = EventBusApiImpl()
        val received = mutableListOf<BusyLibEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            eventBus.getEvents().collect(received::add)
        }

        val events = List(100) { index -> event("device-$index") }
        coroutineScope {
            events.forEach { busyLibEvent ->
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    eventBus.publish(busyLibEvent)
                }
            }
        }

        assertEquals(events.size, received.size)
        assertEquals(events.toSet(), received.toSet())
    }
}
