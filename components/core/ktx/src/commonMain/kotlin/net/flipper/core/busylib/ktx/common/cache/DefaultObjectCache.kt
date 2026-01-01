package net.flipper.core.busylib.ktx.common.cache

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class DefaultObjectCache(
    private val aliveAfterRead: Duration = 5.seconds,
    private val aliveAfterWrite: Duration = Duration.Companion.INFINITE
) : ObjectCache, LogTagProvider by TaggedLogger("DefaultObjectCache") {
    private val mutex = Mutex()
    private val cache = mutableMapOf<KClass<*>, CacheEntry<*>>()

    sealed interface CacheEntry<T : Any> {
        val mutex: Mutex

        data class Pending(override val mutex: Mutex) : CacheEntry<Nothing>

        data class Created<T : Any>(
            val deferredValue: Deferred<T>,
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

    private suspend fun <T : Any> CoroutineScope.replaceEntry(
        clazz: KClass<T>,
        entityMutex: Mutex,
        block: suspend () -> T
    ): CacheEntry.Created<T> {
        val newEntry = CacheEntry.Created(
            deferredValue = async { block.invoke() },
            mutex = entityMutex
        )
        mutex.withLock { cache.put(clazz, newEntry) }
        return newEntry
    }

    override suspend fun <T : Any> getOrElse(
        ignoreCache: Boolean,
        clazz: KClass<T>,
        block: suspend () -> T
    ): Deferred<T> = coroutineScope {
        mutex.withLock {
            clearExpired()
            val entry = cache.getOrPut(clazz) { CacheEntry.Pending(Mutex()) }
            async {
                entry.mutex.withLock {
                    (entry as? CacheEntry.Created<*>)
                        ?.let { entry -> entry.deferredValue as? Deferred<T> }
                        ?.takeIf { !ignoreCache }
                        ?: this@coroutineScope.replaceEntry(
                            clazz = clazz,
                            entityMutex = entry.mutex,
                            block = block
                        ).deferredValue
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
