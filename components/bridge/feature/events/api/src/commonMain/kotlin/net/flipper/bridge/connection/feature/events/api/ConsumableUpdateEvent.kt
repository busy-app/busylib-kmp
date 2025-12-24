package net.flipper.bridge.connection.feature.events.api

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface Consumable {
    /**
     * @return true if it've been consumed, false if already consumed by another consumer
     */
    suspend fun <T> tryConsume(block: suspend (isConsumedSuccessfully: Boolean) -> T): T
}

class DefaultConsumable(val isConsumedSuccessfully: Boolean) : Consumable {
    override suspend fun <T> tryConsume(block: suspend (Boolean) -> T): T {
        return block.invoke(isConsumedSuccessfully)
    }
}

interface ConsumableUpdateEvent : Consumable {
    val updateEvent: UpdateEvent
}

class DefaultConsumableUpdateEvent(override val updateEvent: UpdateEvent) : ConsumableUpdateEvent {
    private var isConsumed = false
    private val mutex = Mutex()
    override suspend fun <T> tryConsume(block: suspend (isConsumedSuccessfully: Boolean) -> T): T {
        return mutex.withLock {
            if (isConsumed) {
                block.invoke(false)
            } else {
                isConsumed = true
                block.invoke(true)
            }
        }
    }
}
