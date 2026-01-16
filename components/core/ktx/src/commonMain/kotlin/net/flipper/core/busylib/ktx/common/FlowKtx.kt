package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

fun <T> Flow<T>?.orEmpty(): Flow<T> = this ?: emptyFlow()

fun <T> Flow<T>.merge(flow: Flow<T>): Flow<T> = listOf(this, flow).merge()

fun <T> Flow<T>?.orNullable(): Flow<T?> = this ?: flowOf(null)

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
