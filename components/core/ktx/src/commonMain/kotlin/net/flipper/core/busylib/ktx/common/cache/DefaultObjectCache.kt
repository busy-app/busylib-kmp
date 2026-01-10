package net.flipper.core.busylib.ktx.common.cache

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class DefaultObjectCache(
    private val aliveAfterRead: Duration = 5.seconds,
    private val aliveAfterWrite: Duration = Duration.INFINITE
) : ObjectCache, LogTagProvider by TaggedLogger("DefaultObjectCache") {
    private val mutex = Mutex()
    private val cache = mutableMapOf<KClass<*>, CacheEntry<*>>()

    sealed interface CacheEntry<T : Any> {

        data object Pending : CacheEntry<Nothing>

        data class Created<T : Any>(
            val deferredValue: Deferred<T>,
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
    private fun clearExpiredUnsafe() {
        cache
            .toMap()
            .filter { (_, value) -> (value as? CacheEntry.Created<*>?)?.isExpired == true }
            .forEach { (clazz, _) -> cache.remove(clazz) }
    }

    private suspend fun <T : Any> CoroutineScope.replaceEntry(
        clazz: KClass<T>,
        block: suspend () -> T
    ): CacheEntry.Created<T> {
        val newEntry = CacheEntry.Created(
            deferredValue = async { block.invoke() },
        )
        cache[clazz] = newEntry
        return newEntry
    }

    override suspend fun <T : Any> getOrElse(
        ignoreCache: Boolean,
        clazz: KClass<T>,
        block: suspend () -> T
    ): Deferred<T> = coroutineScope {
        withContext(NonCancellable) {
            mutex.withLock {
                clearExpiredUnsafe()
                val entry = cache.getOrPut(clazz) { CacheEntry.Pending }
                (entry as? CacheEntry.Created<*>)
                    ?.let { entry -> entry.deferredValue as? Deferred<T> }
                    ?.takeIf { !ignoreCache }
                    ?: this@coroutineScope.replaceEntry(
                        clazz = clazz,
                        block = block
                    ).deferredValue
            }
        }
    }

    override suspend fun clear() {
        mutex.withLock { cache.clear() }
    }
}
