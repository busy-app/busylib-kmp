package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

fun <T> Flow<T>?.orEmpty(): Flow<T> = this ?: emptyFlow()
fun <T> Flow<T>?.orElse(block: () -> T): Flow<T> = this ?: flow { emit(block.invoke()) }

fun <T> Flow<T>.merge(flow: Flow<T>): Flow<T> = listOf(this, flow).merge()

fun <T> Flow<T>?.orNullable(): Flow<T?> = this ?: flowOf(null)

fun <T> SharedFlow<T>.asFlow(): Flow<T> = this.map { value -> value }

inline fun <T, R> Flow<T>.mapCached(
    crossinline transform: suspend (value: T, previous: R?) -> R
): Flow<R> = flow {
    var latest: R? = null
    this@mapCached.collect {
        val current = transform.invoke(it, latest)
        latest = current
        emit(current)
    }
}

inline fun <T, R> Flow<T>.flatMapCached(
    crossinline transform: suspend (value: T, previous: R?) -> Flow<R>
): Flow<R> = flow {
    var latest: R? = null
    this@flatMapCached
        .flatMapLatest { transform.invoke(it, latest) }
        .collect {
            latest = it
            emit(it)
        }
}

inline fun <reified T> List<Flow<T>>.combine(): Flow<List<T>> {
    if (this.isEmpty()) return flowOf(emptyList())
    return combine(
        flows = this,
        transform = { flows -> flows.toList() }
    )
}

inline fun <reified T> Flow<List<List<T>>>.flatten(): Flow<List<T>> {
    return this.map { list -> list.flatten() }
}
