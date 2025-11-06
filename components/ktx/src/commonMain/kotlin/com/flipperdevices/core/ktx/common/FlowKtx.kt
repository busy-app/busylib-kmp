package com.flipperdevices.core.ktx.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

public fun <T> Flow<T>.onLatest(
    action: suspend (T) -> Unit
): Flow<T> = transformLatest { value ->
    action(value)
    return@transformLatest emit(value)
}

inline fun <T, R> Flow<T>.mapCached(
    scope: CoroutineScope,
    started: SharingStarted = SharingStarted.Eagerly,
    dispatcher: CoroutineContext = FlipperDispatchers.default,
    replay: Int = 1,
    crossinline transform: suspend (value: T, previous: R?) -> R
): SharedFlow<R> = flow {
    var latest: R? = null
    this@mapCached.collect {
        val current = transform.invoke(it, latest)
        latest = current
        emit(current)
    }
}.flowOn(dispatcher).shareIn(scope, started, replay)

fun <T> Flow<T>?.orEmpty(): Flow<T> = this ?: emptyFlow()

/**
 * Emits the first value from the flow, applies the given transform
 * and ignores next values while the transform is still executing
 *
 * This is similar to collectLatest, but it doesn't end job when new
 * value is emited
 */
fun <T, K> Flow<T>.throttleFirst(transform: suspend (T) -> K): Flow<K> = channelFlow {
    val scope = this
    var job: Job? = null

    collect { value ->
        if (job?.isActive == true) return@collect
        job = scope.launch {
            send(transform(value))
        }
    }
}
