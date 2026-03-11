package net.flipper.bridge.connection.transport.tcp.lan.impl.monitor.fixture.api

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Tests can [awaitStatusWithType] to suspend until a matching status arrives
 * instead of polling with delay.
 */
class RecordingStatusListener : FTransportConnectionStatusListener {
    private val mutex = Mutex()
    private val _statuses = mutableListOf<FInternalTransportConnectionStatus>()
    val statuses: List<FInternalTransportConnectionStatus>
        get() = _statuses.toList()

    /** Emits every incoming status so awaiters can collect without polling. */
    private val statusFlow = MutableSharedFlow<FInternalTransportConnectionStatus>()

    override suspend fun onStatusUpdate(status: FInternalTransportConnectionStatus) {
        mutex.withLock { _statuses.add(status) }
        statusFlow.emit(status)
    }

    /**
     * Suspend until a status matching [predicate] is recorded.
     * Returns immediately if such a status already exists.
     */
    suspend fun awaitStatus(
        timeout: Duration = 5.seconds,
        predicate: (FInternalTransportConnectionStatus) -> Boolean
    ): FInternalTransportConnectionStatus {
        return statuses.firstOrNull(predicate) // Fast path: already recorded
            ?: withTimeout(
                timeout = timeout,
                block = { statusFlow.first { connectionStatus -> predicate(connectionStatus) } }
            ) // Slow path: wait for the flow
    }

    suspend inline fun <reified T : FInternalTransportConnectionStatus> awaitStatusWithType(
        timeout: Duration = 5.seconds,
    ): FInternalTransportConnectionStatus = awaitStatus(
        timeout = timeout,
        predicate = { connectionStatus -> connectionStatus is T }
    )

    /**
     * Suspend until [count] statuses matching [predicate] have been recorded.
     */
    suspend fun awaitStatusCount(
        count: Int,
        timeout: Duration = 5.seconds,
        predicate: (FInternalTransportConnectionStatus) -> Boolean
    ) {
        if (statuses.count(predicate) >= count) return
        withTimeout(timeout) {
            statusFlow.first { _ -> statuses.count(predicate) >= count }
        }
    }

    /**
     * Assert that no **new** status matching [predicate] arrives within [duration].
     * Only checks statuses emitted after this method is called.
     */
    suspend fun assertNoNewStatus(
        duration: Duration = 2.seconds,
        predicate: (FInternalTransportConnectionStatus) -> Boolean
    ) {
        val arrived = try {
            withTimeout(duration) { statusFlow.first(predicate) }
        } catch (_: TimeoutCancellationException) {
            null
        }
        if (arrived != null) {
            throw AssertionError("Expected no new matching status within $duration but got: $arrived")
        }
    }

    suspend inline fun <reified T : FInternalTransportConnectionStatus> assertNoNewStatusWithType(
        duration: Duration = 2.seconds,
    ) = assertNoNewStatus(
        duration = duration,
        predicate = { connectionStatus -> connectionStatus is T }
    )
}
