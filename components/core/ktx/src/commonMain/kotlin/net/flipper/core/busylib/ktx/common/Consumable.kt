package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Represents a resource that can be consumed exactly one time
 *
 * This interface is useful in event-driven scenarios where we need to ensure
 * that a resource is only processed by a single consumer, and consumed only once,
 * even when multiple subscribers are listening to the same flow or stream
 *
 * ## Example
 * ```kotlin
 * // Prevent multiple listeners from handling the same event
 * consumableFlow.collect { consumable ->
 *     consumable.tryConsume { isConsumedSuccessfully ->
 *         if (isConsumedSuccessfully) {
 *             processEvent()
 *         } else {
 *            // other
 *         }
 *     }
 * }
 * ```
 */
interface Consumable {
    /**
     * Attempts to consume this instance atomically and executes the provided block.
     *
     * The consumption attempt is guaranteed to be atomic - either the current call successfully claims the
     * resource, or it has already been claimed by another consumer. The result is communicated through
     * the block parameter
     *
     * @param block A suspending lambda that receives `true` if this call successfully consumed the instance,
     * or `false` if it was already consumed by another consumer
     */
    suspend fun <T> tryConsume(block: suspend (isConsumedSuccessfully: Boolean) -> T)
}

/**
 * Attempts to consume this instance and returns the result as a Boolean
 *
 * This extension provides a wrapper that returns the consumption result directly,
 * rather than requiring a callback block, in which we'll enter mutex or other internal lock implementation
 *
 * @return `true` if this call successfully consumed the instance, `false` if it was already consumed
 */
suspend fun Consumable.tryConsume(): Boolean {
    return callbackFlow {
        tryConsume { isConsumedSuccessfully ->
            send(isConsumedSuccessfully)
        }
        awaitClose()
    }.first()
}

/**
 * A simple, non-thread-safe implementation of [Consumable] with a fixed consumption result.
 *
 * @param isConsumedSuccessfully The fixed result that will be returned for all consumption attempts
 */
class DefaultConsumable(val isConsumedSuccessfully: Boolean) : Consumable {
    override suspend fun <T> tryConsume(block: suspend (Boolean) -> T) {
        block.invoke(isConsumedSuccessfully)
    }
}

/**
 * A thread-safe implementation of [Consumable] that ensures exactly one consumer succeeds.
 *
 * @param initiallyConsumed If `true`, the consumable starts in a consumed state and all calls
 * will receive `false`. If `false` (default), the first consumer succeeds.
 */
class MutexConsumable(initiallyConsumed: Boolean = false) : Consumable {
    private var isConsumed = initiallyConsumed
    private val mutex = Mutex()
    override suspend fun <T> tryConsume(block: suspend (isConsumedSuccessfully: Boolean) -> T) {
        // Don't enter mutex if already consumed
        if (isConsumed) {
            block.invoke(false)
            return
        }
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
