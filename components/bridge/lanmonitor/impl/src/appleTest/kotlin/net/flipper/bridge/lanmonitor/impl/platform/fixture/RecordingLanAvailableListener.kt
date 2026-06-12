package net.flipper.bridge.lanmonitor.impl.platform.fixture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Records every `Boolean` emitted by [net.flipper.bridge.lanmonitor.impl.platform.LanAvailableMacOSListener]'s
 * LAN-availability flow so tests can suspend until a matching value arrives instead of polling with delay.
 *
 * `true` means the device is reachable (LAN available); `false` means preparing / waiting / restarting.
 *
 * History is kept in a single append-only [MutableStateFlow]. Because a [kotlinx.coroutines.flow.StateFlow]
 * always replays its current value to new subscribers, an awaiter that subscribes *after* a value was recorded
 * still observes it — there is no lost-notification window. The listener emits each availability transition
 * exactly once (e.g. a single `true` on `Ready`, then nothing until the next state change), so a lossy
 * notification would otherwise leave [awaitValue] blocked until its timeout.
 */
class RecordingLanAvailableListener {
    private val valuesState = MutableStateFlow<List<Boolean>>(emptyList())

    val values: List<Boolean>
        get() = valuesState.value

    fun record(value: Boolean) {
        valuesState.update { recorded -> recorded + value }
    }

    /**
     * Suspend until a value matching [predicate] is recorded.
     * Returns immediately if such a value already exists.
     */
    suspend fun awaitValue(
        timeout: Duration = 5.seconds,
        predicate: (Boolean) -> Boolean
    ): Boolean = withTimeout(timeout) {
        valuesState
            .mapNotNull { recorded -> recorded.firstOrNull(predicate) }
            .first()
    }

    /**
     * Suspend until [count] values matching [predicate] have been recorded.
     */
    suspend fun awaitValueCount(
        count: Int,
        timeout: Duration = 5.seconds,
        predicate: (Boolean) -> Boolean
    ) {
        withTimeout(timeout) {
            valuesState.first { recorded -> recorded.count(predicate) >= count }
        }
    }

    /**
     * Assert that no **new** value matching [predicate] arrives within [duration].
     * Only checks values recorded after this method is called.
     */
    suspend fun assertNoNewValue(
        duration: Duration = 2.seconds,
        predicate: (Boolean) -> Boolean
    ) {
        val baseline = valuesState.value.size
        val arrived = withTimeoutOrNull(duration) {
            valuesState
                .mapNotNull { recorded -> recorded.drop(baseline).firstOrNull(predicate) }
                .first()
        }
        if (arrived != null) {
            throw AssertionError("Expected no new matching value within $duration but got: $arrived")
        }
    }
}
