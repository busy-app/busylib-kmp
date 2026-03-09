package net.flipper.bridge.connection.feature.events.model

import net.flipper.core.busylib.ktx.common.Consumable
import net.flipper.core.busylib.ktx.common.DefaultConsumable
import net.flipper.core.busylib.ktx.common.MutexConsumable

/**
 * Wraps an [BsbUpdateEvent] in a [Consumable] so that it can be delivered at most once
 *
 * @see Consumable
 */
sealed interface ConsumableUpdateEvent : Consumable {
    data class Bsb(
        val bsbUpdateEvent: BsbUpdateEvent,
        val value: String?
    ) : ConsumableUpdateEvent,
        Consumable by MutexConsumable()

    data class BusyLib<out T : BusyLibUpdateEvent>(
        val busyLibUpdateEvent: T,
    ) : ConsumableUpdateEvent,
        Consumable by MutexConsumable()

    data object Empty : ConsumableUpdateEvent, Consumable by DefaultConsumable(false)
}
