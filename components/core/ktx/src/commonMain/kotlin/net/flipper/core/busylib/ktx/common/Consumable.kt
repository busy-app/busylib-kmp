package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface Consumable {
    /**
     * @return true if it have been consumed, false if already consumed by another consumer
     */
    suspend fun <T> tryConsume(block: suspend (isConsumedSuccessfully: Boolean) -> T): T
}

class DefaultConsumable(val isConsumedSuccessfully: Boolean) : Consumable {
    override suspend fun <T> tryConsume(block: suspend (Boolean) -> T): T {
        return block.invoke(isConsumedSuccessfully)
    }
}

class MutexConsumable(initiallyConsumed: Boolean = false) : Consumable {
    private var isConsumed = initiallyConsumed
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
