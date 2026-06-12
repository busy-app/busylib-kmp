package net.flipper.eventbus.model

import net.flipper.bridge.connection.config.api.model.BUSYBar

/**
 * Events emitted by the BUSY library to notify native consumers about notable,
 * momentary occurrences. Consumers subscribe via `net.flipper.eventbus.api.EventBusApi`.
 *
 * These events are transient: an event emitted while there is no active subscriber
 * is dropped, not replayed.
 */
sealed interface BusyLibEvent {
    /**
     * The active device was switched automatically by the library, without an explicit
     * user action. [newDevice] is the device that is now active.
     */
    data class ActiveDeviceAutoSwitched(val newDevice: BUSYBar) : BusyLibEvent
}
