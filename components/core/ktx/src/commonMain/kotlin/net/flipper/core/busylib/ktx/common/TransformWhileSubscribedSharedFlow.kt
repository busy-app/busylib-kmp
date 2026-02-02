package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.debug
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
private class TransformWhileSubscribedSharedFlow<T, R>(
    private val scope: CoroutineScope,
    private val upstreamFlow: Flow<T>,
    private val timeoutDuration: Duration = 5.seconds,
    private val collector: suspend Flow<T>.(collector: FlowCollector<R>) -> Unit,
) : SharedFlow<R>, LogTagProvider by TaggedLogger("ConditionalTimeoutSharedFlow") {
    private val resultFlow = MutableSharedFlow<R>(replay = 1, extraBufferCapacity = 0)

    override val replayCache: List<R>
        get() = resultFlow.replayCache
    private val subscriberMutex = Mutex()
    private var subscriberCount = 0
    private var upstreamJob: Job? = null
    private var timeoutJob: Job? = null

    private suspend fun startUpstreamCollection() {
        upstreamJob?.cancelAndJoin()
        upstreamJob = scope.launch {
            collector.invoke(upstreamFlow) { value ->
                val shouldEmit = subscriberMutex.withLock { subscriberCount > 0 }
                if (shouldEmit) {
                    resultFlow.emit(value)
                }
            }
        }
    }

    private suspend fun stopUpstreamCollection() {
        debug {
            "#stopUpstreamCollection Stopping upstream collection " +
                "after $timeoutDuration seconds of no subscribers"
        }
        upstreamJob?.cancelAndJoin()
        upstreamJob = null
        resultFlow.resetReplayCache()
    }

    private suspend fun startTimeout() {
        timeoutJob?.cancelAndJoin()
        timeoutJob = scope.launch {
            delay(timeoutDuration)
            stopUpstreamCollection()
        }
    }

    private suspend fun cancelTimeout() {
        timeoutJob?.cancelAndJoin()
        timeoutJob = null
    }

    override suspend fun collect(collector: FlowCollector<R>): Nothing {
        subscriberMutex.withLock { subscriberCount++ }
        debug { "#collect Subscriber added. Count: $subscriberCount" }

        try {
            cancelTimeout()

            if (upstreamJob?.isActive != true) {
                debug { "#collect Starting upstream collection due to new subscriber" }
                startUpstreamCollection()
            }

            resultFlow.collect(collector)
        } finally {
            subscriberMutex.withLock {
                subscriberCount--
                debug { "#collect Subscriber removed. Count: $subscriberCount" }
                if (subscriberCount == 0) {
                    startTimeout()
                }
            }
        }
    }
}

/**
 * This operator is useful for scenarios where you want to:
 * - Avoid wasting resources collecting from upstream when no one is listening
 * - Provide a grace period for subscribers to reconnect without restarting upstream
 * - Transform values during collection while controlling emission timing
 *
 * ## Behavior
 *
 * **Upstream Collection:**
 * - Starts when the first subscriber connects
 * - Continues running while subscribers are present
 * - Keeps running for [timeout] duration after the last subscriber disconnects (grace period)
 * - Stops completely after the timeout expires with no subscribers
 * - Restarts when a new subscriber connects after being stopped
 *
 * **Emission Strategy:**
 * - Values are only emitted to subscribers when at least one subscriber is present
 * - The latest value is always cached via `replay=1`, even when there are no subscribers
 * - Cached value is immediately delivered to new subscribers upon connection
 * - The replay cache is cleared after the timeout expires
 *
 * **Transformation:**
 * - The [collector] lambda allows custom transformation logic during collection
 * - Use `collect { emit(transform(it)) }` for simple transformations
 * - Use `collectLatest { emit(transform(it)) }` for conflated values
 */
@DelicateBusyLibApi
fun <T, R> Flow<T>.transformWhileSubscribed(
    timeout: Duration = 30.seconds,
    scope: CoroutineScope,
    transformFlow: (flow: Flow<T>) -> Flow<R>,
): SharedFlow<R> {
    return TransformWhileSubscribedSharedFlow(
        timeoutDuration = timeout,
        upstreamFlow = this,
        scope = scope,
        collector = { collector -> transformFlow.invoke(this).collect(collector) }
    )
}
