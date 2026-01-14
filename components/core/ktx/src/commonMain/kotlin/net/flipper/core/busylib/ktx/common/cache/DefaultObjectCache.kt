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
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class DefaultObjectCache(
    private val aliveAfterRead: Duration = 15.seconds,
    private val aliveAfterWrite: Duration = Duration.INFINITE,
    private val timeProvider: TimeProvider = SystemTimeProvider()
) : ObjectCache, LogTagProvider by TaggedLogger("DefaultObjectCache") {
    private val mutex = Mutex()
    private val cache = mutableMapOf<KClass<*>, CacheEntry<*>>()

    sealed interface CacheEntry<T : Any> {

        data object Pending : CacheEntry<Nothing>

        data class Created<T : Any>(
            val deferredValue: Deferred<T>,
            val lastReadAt: ComparableTimeMark,
            val writtenAt: ComparableTimeMark
        ) : CacheEntry<T>
    }

    private val CacheEntry.Created<*>.isExpired: Boolean
        get() {
            return this.lastReadAt.plus(aliveAfterRead)
                .hasPassedNow()
                .or(this.writtenAt.plus(aliveAfterWrite).hasPassedNow())
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

    private fun <T : Any> putEntry(
        clazz: KClass<T>,
        scope: CoroutineScope,
        block: suspend () -> T
    ): CacheEntry.Created<T> {
        val now = timeProvider.markNow()
        val newEntry = CacheEntry.Created(
            deferredValue = scope.async { block.invoke() },
            lastReadAt = now,
            writtenAt = now
        )
        cache[clazz] = newEntry
        return newEntry
    }

    /**
     * @return entry and update its [CacheEntry.Created.lastReadAt]
     */
    private fun getEntry(clazz: KClass<*>): CacheEntry<*> {
        val entry = cache.getOrPut(clazz) { CacheEntry.Pending }
        val newEntry = when (entry) {
            is CacheEntry.Created<*> -> {
                entry.copy(lastReadAt = timeProvider.markNow())
            }

            CacheEntry.Pending -> entry
        }
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
                val entry = getEntry(clazz)
                (entry as? CacheEntry.Created<*>)
                    ?.let { entry -> entry.deferredValue as? Deferred<T> }
                    ?.takeIf { !ignoreCache }
                    ?: putEntry(
                        clazz = clazz,
                        scope = this,
                        block = block
                    ).deferredValue
            }
        }
    }

    override suspend fun clear() {
        mutex.withLock { cache.clear() }
    }
}
