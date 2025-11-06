package com.flipperdevices.core.ktx.common

import kotlin.coroutines.cancellation.CancellationException

suspend fun <T, R> Result<T>.transform(block: suspend (T) -> Result<R>): Result<R> {
    return mapCatching { firstResult ->
        block(firstResult)
    }.mapCatching { it.getOrThrow() }
}

suspend inline fun <R> runSuspendCatching(block: suspend () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (c: CancellationException) {
        throw c
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
