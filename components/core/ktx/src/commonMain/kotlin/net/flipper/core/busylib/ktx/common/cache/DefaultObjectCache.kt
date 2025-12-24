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

    private val mutex = Mutex()

    private suspend fun <T : Any> createEntry(
        clazz: KClass<T>,
        entry: CacheEntry<*>,
        block: suspend () -> T
    ): CacheEntry.Created<T> {
        val newEntry = CacheEntry.Created(
            value = block.invoke(),
            mutex = entry.mutex
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
            val entry = cache.getOrPut(clazz) { CacheEntry.Pending(Mutex()) }
            async {
                entry.mutex.withLock {
                    when (entry) {
                        is CacheEntry.Created<*> -> {
                            if (entry.isExpired || ignoreCache) {
                                createEntry(
                                    clazz = clazz,
                                    entry = entry,
                                    block = block
                                ).value
                            } else {
                                entry.value as T
                            }
                        }

                        is CacheEntry.Pending -> {
                            createEntry(
                                clazz = clazz,
                                entry = entry,
                                block = block
                            ).value
                        }
                    }
                }
            }
        }.await()
    }

    override suspend fun clear() {
        mutex.withLock {
            val mutexes = cache.map { it.value.mutex }
            mutexes.forEach { mutex -> mutex.lock() }
            cache.clear()
            mutexes.forEach { mutex -> mutex.unlock() }
        }
    }
}
