package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
) : SharedFlow<R>, LogTagProvider by TaggedLogger("TransformWhileSubscribedSharedFlow") {
    private val resultFlow = MutableSharedFlow<R>(replay = 1, extraBufferCapacity = 0)

    override val replayCache: List<R>
        get() = resultFlow.replayCache
    private val subscriberMutex = Mutex()
    private var subscriberCountFlow = MutableStateFlow(0)
    private val hasSubscribersFlow = subscriberCountFlow
        .map { count -> count > 0 }
        .distinctUntilChanged()
    private var upstreamJob: Job? = null
    private var timeoutJob: Job? = null
    private suspend fun awaitForSubscribers() {
        hasSubscribersFlow
            .filter { hasSubscribers -> hasSubscribers }
            .first()
    }
    private suspend fun startUpstreamCollectionUnsafe() {
        if (subscriberCountFlow.first() == 0) return
        upstreamJob?.cancelAndJoin()
        upstreamJob = scope.launch {
            collector.invoke(
                upstreamFlow.mapLatest { upstreamValue ->
                    awaitForSubscribers()
                    upstreamValue
                },
            ) { value -> resultFlow.emit(value) }
        }
    }

    private suspend fun stopUpstreamCollectionUnsafe() {
        if (subscriberCountFlow.first() > 0) return
        debug {
            "#stopUpstreamCollection Stopping upstream collection " +
                "after $timeoutDuration seconds of no subscribers"
        }
        upstreamJob?.cancelAndJoin()
        upstreamJob = null
        resultFlow.resetReplayCache()
    }

    private suspend fun startTimeoutUnsafe() {
        if (subscriberCountFlow.first() > 0) return
        timeoutJob?.cancelAndJoin()
        timeoutJob = scope.launch {
            debug { "#startTimeout $timeoutDuration" }
            delay(timeoutDuration)
            subscriberMutex.withLock { stopUpstreamCollectionUnsafe() }
        }
    }

    private suspend fun cancelTimeoutUnsafe() {
        if (subscriberCountFlow.first() == 0 && timeoutJob == null) return
        debug { "#cancelTimeout" }
        timeoutJob?.cancelAndJoin()
        timeoutJob = null
    }

    private suspend fun onSubscriberAddedUnsafe() {
        val count = subscriberCountFlow.updateAndGet { count -> count + 1 }
        cancelTimeoutUnsafe()

        debug { "#onSubscriberAddedUnsage Subscriber added. Count: $count" }

        if (upstreamJob?.isActive != true) {
            debug { "#onSubscriberAddedUnsage Starting upstream collection due to new subscriber" }
            startUpstreamCollectionUnsafe()
        }
    }

    private suspend fun onSubscriberRemovedUnsafe() {
        val count = subscriberCountFlow.updateAndGet { count -> count - 1 }
        debug { "#onSubscriberRemovedUnsafe Subscriber removed. Count: $count" }
        if (count == 0) {
            startTimeoutUnsafe()
        }
    }

    override suspend fun collect(collector: FlowCollector<R>): Nothing {
        try {
            subscriberMutex.withLock { onSubscriberAddedUnsafe() }
            resultFlow.collect(collector)
        } finally {
            withContext(NonCancellable) {
                subscriberMutex.withLock { onSubscriberRemovedUnsafe() }
            }
        }
    }
}

/**
 * This operator is useful for scenarios where you want to:
 * - Don't process values when no subscribers are present
 * - Provide a grace period for subscribers to reconnect without restarting upstream
 * - Transform values during collection while controlling emission timing
 *
 * Instead of immediate transform execution when there's no subscribers,
 * this operator will await for subscribers and only after that,
 * it will transform the latest value from original flow. This is the key difference from `shareIn(WhileSubscribed)`
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
 * - The latest value is cached via `replay = 1` while upstream is active and
 * during the grace period, even if there are temporarily no subscribers
 * - Cached value is immediately delivered to new subscribers upon connection during this period
 * - After the timeout expires with no subscribers, upstream stops and the replay cache is cleared
 *
 * **Transformation:**
 * - The [transformFlow] lambda allows custom transformation logic during collection
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
