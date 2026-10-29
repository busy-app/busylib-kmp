package net.flipper.eventbus.model

import net.flipper.bridge.connection.config.api.model.BUSYBar
import kotlin.time.Instant

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

    /**
     * The platform denied access to the local network (e.g. Apple "Local Network" permission).
     * Emitted when LAN monitoring cannot proceed until the user grants access.
     */
    data object LocalNetworkDenied : BusyLibEvent

    /**
     * The cloud granted or revoked the BUSY Bar owner access for the current user.
     * [graceUntil] is set while the access is kept for a grace period after the last bar was unlinked.
     * Consumers should refresh the entitlements from the purchase provider.
     */
    data class BusyBarAccessUpdated(
        val granted: Boolean,
        val graceUntil: Instant?
    ) : BusyLibEvent
}
