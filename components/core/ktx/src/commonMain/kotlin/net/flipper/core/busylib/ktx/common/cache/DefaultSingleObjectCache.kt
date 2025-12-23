package net.flipper.core.busylib.ktx.common.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class DefaultSingleObjectCache<T : Any>(
    private val aliveAfterRead: Duration = 5.seconds,
    private val aliveAfterWrite: Duration = Duration.Companion.INFINITE
) : SingleObjectCache<T> {
    private sealed interface Entry<out T : Any> {
        data object Empty : Entry<Nothing>
        data class Present<T : Any>(
            val value: T,
            val writtenAt: TimeSource.Monotonic.ValueTimeMark,
            var lastReadAt: TimeSource.Monotonic.ValueTimeMark,
            val key: Any?
        ) : Entry<T>
    }

    private val mutex = Mutex()
    private val clock = TimeSource.Monotonic

    private val entryFlow = MutableStateFlow<Entry<T>>(Entry.Empty)
    override val flow: Flow<T> = entryFlow
        .filterIsInstance<Entry.Present<T>>()
        .map { entry -> entry.value }

    private fun tryClear() {
        entryFlow.update { entry ->
            when (entry) {
                is Entry.Present<*> -> {
                    val shouldBeCleared = entry.writtenAt.plus(aliveAfterWrite).hasPassedNow()
                        .or(entry.lastReadAt.plus(aliveAfterRead).hasPassedNow())
                    if (shouldBeCleared) {
                        Entry.Empty
                    } else {
                        entry
                    }
                }

                else -> entry
            }
        }
    }

    private suspend fun createNewEntry(
        key: Any?,
        block: suspend () -> T
    ): Entry.Present<T> {
        val newEntry = Entry.Present(
            value = block.invoke(),
            writtenAt = clock.markNow(),
            lastReadAt = clock.markNow(),
            key = key
        )
        entryFlow.update { newEntry }
        return newEntry
    }

    override suspend fun getOrElse(
        key: Any?,
        block: suspend () -> T
    ): T = mutex.withLock {
        tryClear()
        val currentEntry = entryFlow.first()

        if (currentEntry !is Entry.Present<T>) {
            createNewEntry(key, block).value
        } else {
            if (key == null || key == currentEntry.key) {
                currentEntry.lastReadAt = clock.markNow()
                currentEntry.value
            } else {
                createNewEntry(key, block).value
            }
        }
    }

    override suspend fun clear() {
        mutex.withLock { entryFlow.update { Entry.Empty } }
    }
}
