package net.flipper.bridge.connection.feature.events.api

import net.flipper.core.busylib.ktx.common.Consumable
import net.flipper.core.busylib.ktx.common.MutexConsumable

class ConsumableUpdateEvent(val updateEvent: UpdateEvent) : Consumable by MutexConsumable()
