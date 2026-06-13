package net.flipper.eventbus.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.MutableSharedFlow
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.WrappedSharedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.verbose
import net.flipper.eventbus.api.EventBusApi
import net.flipper.eventbus.internal.BusyLibEventPublisher
import net.flipper.eventbus.model.BusyLibEvent

@SingleIn(BusyLibGraph::class)
@Inject
@ContributesBinding(BusyLibGraph::class, binding = binding<EventBusApi>())
@ContributesBinding(BusyLibGraph::class, binding = binding<BusyLibEventPublisher>())
class EventBusApiImpl : EventBusApi, BusyLibEventPublisher, LogTagProvider {
    override val TAG: String = "EventBus"

    // replay = 0 with no buffer: emit drops the event when there is no subscriber,
    // and suspends (backpressure) while a subscriber is collecting it.
    private val events = MutableSharedFlow<BusyLibEvent>()

    override fun getEvents(): WrappedSharedFlow<BusyLibEvent> = events.wrap()

    override suspend fun publish(event: BusyLibEvent) {
        verbose { "#publish $event" }
        events.emit(event)
    }
}
