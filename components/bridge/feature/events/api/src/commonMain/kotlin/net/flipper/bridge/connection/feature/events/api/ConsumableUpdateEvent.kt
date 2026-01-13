package net.flipper.bridge.connection.feature.events.api

import net.flipper.core.busylib.ktx.common.Consumable
import net.flipper.core.busylib.ktx.common.MutexConsumable

/**
 * Wraps an [UpdateEvent] in a [Consumable] so that it can be delivered at most once
 *
 * @see Consumable
 */
class ConsumableUpdateEvent(val updateEvent: UpdateEvent) : Consumable by MutexConsumable()
