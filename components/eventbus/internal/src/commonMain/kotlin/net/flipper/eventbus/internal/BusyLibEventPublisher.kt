package net.flipper.eventbus.internal

import net.flipper.eventbus.model.BusyLibEvent

/**
 * Internal publish side of the event bus. Library components inject this to notify
 * consumers. It is intentionally not exported, so native consumers cannot publish.
 */
interface BusyLibEventPublisher {
    /**
     * Emits [event] to current subscribers. Suspends while a subscriber is receiving
     * (backpressure). If there is no subscriber, the event is dropped.
     */
    suspend fun publish(event: BusyLibEvent)
}
