package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

/**
 * Unlike mapLatest, [throttleLatest] will compute next flow only when previous computation is completed
 */
fun <T, K> Flow<T>.throttleLatest(
    transform: suspend (T) -> K
): Flow<K> = channelFlow {
    val scope = this
    var job: Job? = null

    collectLatest { value ->
        job?.join()
        job = scope.launch {
            send(transform(value))
        }
    }
}

public fun <T> Flow<T>.onLatest(
    action: suspend (T) -> Unit
): Flow<T> = transformLatest { value ->
    action(value)
    return@transformLatest emit(value)
}
