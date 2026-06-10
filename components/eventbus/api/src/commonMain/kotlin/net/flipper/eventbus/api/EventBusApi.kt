package net.flipper.eventbus.api

import net.flipper.busylib.core.wrapper.WrappedSharedFlow
import net.flipper.eventbus.model.BusyLibEvent

/**
 * Consumer-facing entry point of the event bus. Consumers may only observe events;
 * publishing is an internal capability hidden behind `BusyLibEventPublisher`.
 */
interface EventBusApi {
    /**
     * A hot stream of [BusyLibEvent]s. Events are transient (no replay): a subscriber
     * receives only events emitted while it is collecting.
     */
    fun getEvents(): WrappedSharedFlow<BusyLibEvent>
}
