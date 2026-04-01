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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
private class TransformWhileSubscribedSharedFlow<T, R>(
    upstreamFlow: Flow<T>,
    private val scope: CoroutineScope,
    private val timeoutDuration: Duration,
    private val collector: suspend Flow<T>.(collector: FlowCollector<R>) -> Unit,
) : SharedFlow<R>, LogTagProvider by TaggedLogger("TransformWhileSubscribedSharedFlow") {
    private val resultFlow = MutableSharedFlow<R>(replay = 1, extraBufferCapacity = 0)

    override val replayCache: List<R>
        get() = resultFlow.replayCache

    private val upstreamSharedFlow = upstreamFlow
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(
                stopTimeout = timeoutDuration,
                replayExpiration = timeoutDuration
            ),
            replay = 1
        )

    private val subscriberMutex = Mutex()
    private val hasSubscribersFlow = resultFlow.subscriptionCount
        .map { count -> count != 0 }
        .distinctUntilChanged()
    private var transformFlowCollectorJob: Job? = null
    private var replayCacheResetJob: Job? = null

    private suspend fun startTransformFlowCollectionUnsafe() {
        debug { "#startTransformFlowCollectionUnsafe" }
        transformFlowCollectorJob?.cancelAndJoin()
        transformFlowCollectorJob = scope.launch {
            try {
                collector.invoke(
                    upstreamSharedFlow,
                    resultFlow
                )
            } catch (e: Exception) {
                error(e) { "#startTransformFlowCollectionUnsafe" }
            }
        }
    }

    private suspend fun stopTransformFlowCollectionUnsafe() {
        debug { "#stopTransformFlowCollectionUnsafe Stopping transform collection" }
        transformFlowCollectorJob?.cancelAndJoin()
        transformFlowCollectorJob = null
    }

    private suspend fun startReplayCacheResetJobUnsafe() {
        debug { "#startReplayCacheResetJobUnsafe" }
        replayCacheResetJob?.cancelAndJoin()
        replayCacheResetJob = scope.launch {
            delay(timeoutDuration)
            resultFlow.resetReplayCache()
            upstreamSharedFlow.replayCache
        }
    }

    private suspend fun stopReplayCacheResetJobUnsafe() {
        debug { "#stopReplayCacheResetJobUnsafe" }
        replayCacheResetJob?.cancelAndJoin()
        replayCacheResetJob = null
    }

    private suspend fun onSubscriberAddedUnsafe() {
        val count = resultFlow.subscriptionCount.first()
        debug { "#onSubscriberAddedUnsage Subscriber added. Count: $count" }
        if (transformFlowCollectorJob?.isActive == true) return

        debug { "#onSubscriberAddedUnsage Starting upstream collection due to new subscriber" }
        stopReplayCacheResetJobUnsafe()
        startTransformFlowCollectionUnsafe()
    }

    private suspend fun onSubscriberRemovedUnsafe() {
        val count = resultFlow.subscriptionCount.first()
        debug { "#onSubscriberRemovedUnsafe Subscriber removed. Count: $count" }
        if (count > 0) return
        stopTransformFlowCollectionUnsafe()
        startReplayCacheResetJobUnsafe()
    }

    private fun startSubscriberCountJob() {
        hasSubscribersFlow
            .onEach { hasSubscribers ->
                if (hasSubscribers) {
                    subscriberMutex.withLock { onSubscriberAddedUnsafe() }
                } else {
                    subscriberMutex.withLock { onSubscriberRemovedUnsafe() }
                }
            }
            .launchIn(scope)
    }

    override suspend fun collect(collector: FlowCollector<R>): Nothing {
        resultFlow.collect(collector)
    }

    init {
        startSubscriberCountJob()
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
