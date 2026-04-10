package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

suspend fun <T, R> Result<T>.transform(block: suspend (T) -> Result<R>): Result<R> {
    return mapCatching { firstResult ->
        block(firstResult)
    }.mapCatching { it.getOrThrow() }
}

suspend inline fun <R> runSuspendCatching(
    dispatcher: CoroutineDispatcher? = null,
    crossinline block: suspend () -> R
): Result<R> {
    return try {
        val result = if (dispatcher != null) {
            withContext(dispatcher) {
                block()
            }
        } else {
            block()
        }
        Result.success(result)
    } catch (c: CancellationException) {
        throw c
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
