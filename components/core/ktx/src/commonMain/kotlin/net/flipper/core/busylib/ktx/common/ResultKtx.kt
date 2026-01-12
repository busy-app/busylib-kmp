package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

suspend fun <T, R> Result<T>.transform(block: suspend (T) -> Result<R>): Result<R> {
    return mapCatching { firstResult ->
        block(firstResult)
    }.mapCatching { it.getOrThrow() }
}

suspend inline fun <R> runSuspendCatching(
    dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
    crossinline block: suspend () -> R
): Result<R> {
    return try {
        val result = withContext(dispatcher) {
            block()
        }
        Result.success(result)
    } catch (c: CancellationException) {
        throw c
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
