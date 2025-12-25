package net.flipper.core.busylib.ktx.common.cache

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class DefaultObjectCache(
    private val aliveAfterRead: Duration = 5.seconds,
    private val aliveAfterWrite: Duration = Duration.Companion.INFINITE
) : ObjectCache {
    private val mutex = Mutex()
    private val cache = mutableMapOf<KClass<*>, CacheEntry<*>>()

    sealed interface CacheEntry<T : Any> {
        val mutex: Mutex

        data class Pending(override val mutex: Mutex) : CacheEntry<Nothing>

        data class Created<T : Any>(
            val value: T,
            override val mutex: Mutex
        ) : CacheEntry<T> {
            val writtenAt: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow()
            var lastReadAt: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow()
        }
    }

    private val CacheEntry.Created<*>.isExpired: Boolean
        get() {
            return this.lastReadAt.plus(aliveAfterRead)
                .hasPassedNow()
                .and(this.writtenAt.plus(aliveAfterWrite).hasPassedNow())
        }

    /**
     * Clear expired entries only if entry's mutex is not locked
     */
    private fun clearExpired() {
        cache
            .toMap()
            .filter { (_, value) -> (value as? CacheEntry.Created<*>?)?.isExpired == true }
            .filter { (_, value) -> value.mutex.tryLock() }
            .forEach { (clazz, value) ->
                cache.remove(clazz)
                value.mutex.unlock()
            }
    }

    private suspend fun <T : Any> replaceEntry(
        clazz: KClass<T>,
        mutex: Mutex,
        block: suspend () -> T
    ): CacheEntry.Created<T> {
        val newEntry = CacheEntry.Created(
            value = block.invoke(),
            mutex = mutex
        )
        mutex.withLock { cache.put(clazz, newEntry) }
        return newEntry
    }

    override suspend fun <T : Any> getOrElse(
        ignoreCache: Boolean,
        clazz: KClass<T>,
        block: suspend () -> T
    ): T = coroutineScope {
        mutex.withLock {
            clearExpired()
            val entry = cache.getOrPut(clazz) { CacheEntry.Pending(Mutex()) }
            async {
                entry.mutex.withLock {
                    (entry as? CacheEntry.Created<T>)
                        ?.value
                        ?.takeIf { !ignoreCache }
                        ?: replaceEntry(
                            clazz = clazz,
                            mutex = entry.mutex,
                            block = block
                        ).value
                }
            }
        }.await()
    }

    override suspend fun clear() {
        mutex.withLock {
            val mutexes = cache.map { (_, entry) -> entry.mutex }
            mutexes.forEach { mutex -> mutex.lock() }
            cache.clear()
            mutexes.forEach { mutex -> mutex.unlock() }
        }
    }
}
