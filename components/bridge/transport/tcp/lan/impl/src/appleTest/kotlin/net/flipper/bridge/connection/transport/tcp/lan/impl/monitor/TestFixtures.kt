package net.flipper.bridge.connection.transport.tcp.lan.impl.monitor

import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.bridge.connection.transport.tcp.lan.FLanApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Thread-safe status recorder. The Network.framework callbacks come from a
 * dispatch queue via `runBlocking`, so concurrent access is possible.
 *
 * Tests can [awaitStatus] to suspend until a matching status arrives
 * instead of polling with delay.
 */
class RecordingStatusListener : FTransportConnectionStatusListener {
    private val mutex = Mutex()
    private val _statuses = mutableListOf<FInternalTransportConnectionStatus>()
    val statuses: List<FInternalTransportConnectionStatus> get() = _statuses.toList()

    /** Emits every incoming status so awaiters can collect without polling. */
    private val statusFlow = MutableSharedFlow<FInternalTransportConnectionStatus>(extraBufferCapacity = 64)

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
        // Fast path: already recorded
        statuses.firstOrNull(predicate)?.let { return it }
        // Slow path: wait for the flow
        return withTimeout(timeout) {
            statusFlow.first { predicate(it) }
        }
    }

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
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            null
        }
        if (arrived != null) {
            throw AssertionError("Expected no new matching status within $duration but got: $arrived")
        }
    }
}

class FakeLanApi : FLanApi {
    override val deviceName: String = "TestDevice"

    override suspend fun tryUpdateConnectionConfig(
        config: FDeviceConnectionConfig<*>
    ): Result<Unit> = Result.success(Unit)

    override suspend fun disconnect() = Unit

    override fun getDeviceHttpEngine(): HttpClientEngine {
        error("Not implemented in test")
    }

    override fun getCapabilities(): Flow<List<FHTTPTransportCapability>> = flowOf(emptyList())
}
