package net.flipper.bridge.lanmonitor.impl.platform.fixture

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Records every `Boolean` emitted by [net.flipper.bridge.lanmonitor.impl.platform.LanAvailableMacOSListener]'s
 * LAN-availability flow so tests can suspend until a matching value arrives instead of polling with delay.
 *
 * `true` means the device is reachable (LAN available); `false` means preparing / waiting / restarting.
 */
class RecordingLanAvailableListener {
    private val mutex = Mutex()
    private val _values = mutableListOf<Boolean>()
    val values: List<Boolean>
        get() = _values.toList()

    /** Re-emits every recorded value so awaiters can collect without polling. */
    private val valueFlow = MutableSharedFlow<Boolean>()

    suspend fun record(value: Boolean) {
        mutex.withLock { _values.add(value) }
        valueFlow.emit(value)
    }

    /**
     * Suspend until a value matching [predicate] is recorded.
     * Returns immediately if such a value already exists.
     */
    suspend fun awaitValue(
        timeout: Duration = 5.seconds,
        predicate: (Boolean) -> Boolean
    ): Boolean {
        return values.firstOrNull(predicate) // Fast path: already recorded
            ?: withTimeout(
                timeout = timeout,
                block = { valueFlow.first { value -> predicate(value) } }
            ) // Slow path: wait for the flow
    }

    /**
     * Suspend until [count] values matching [predicate] have been recorded.
     */
    suspend fun awaitValueCount(
        count: Int,
        timeout: Duration = 5.seconds,
        predicate: (Boolean) -> Boolean
    ) {
        if (values.count(predicate) >= count) return
        withTimeout(timeout) {
            valueFlow.first { _ -> values.count(predicate) >= count }
        }
    }

    /**
     * Assert that no **new** value matching [predicate] arrives within [duration].
     * Only checks values emitted after this method is called.
     */
    suspend fun assertNoNewValue(
        duration: Duration = 2.seconds,
        predicate: (Boolean) -> Boolean
    ) {
        val arrived = try {
            withTimeout(duration) { valueFlow.first(predicate) }
        } catch (_: TimeoutCancellationException) {
            null
        }
        if (arrived != null) {
            throw AssertionError("Expected no new matching value within $duration but got: $arrived")
        }
    }
}
