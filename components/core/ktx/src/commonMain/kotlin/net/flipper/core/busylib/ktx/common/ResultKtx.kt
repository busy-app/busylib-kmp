package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

suspend inline fun <T, R> Result<T>.mapSuspendCatching(
    crossinline transform: suspend (T) -> R
): Result<R> {
    val value = getOrElse {
        if (it is CancellationException) throw it
        return Result.failure(it)
    }
    return runSuspendCatching { transform(value) }
}

suspend fun <T, R> Result<T>.transform(block: suspend (T) -> Result<R>): Result<R> {
    return mapSuspendCatching { block(it).getOrThrow() }
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

/**
 * @return failure for a [CancellationException] raised inside [block] while the caller is still active
 * (a `withTimeout` expiry, a `Deferred` completed exceptionally); the caller's own cancellation is rethrown
 */
suspend inline fun <R> runSuspendCatchingUnlessCancelled(
    crossinline block: suspend () -> R
): Result<R> {
    return try {
        runSuspendCatching { block() }
    } catch (cancellation: CancellationException) {
        currentCoroutineContext().ensureActive()
        Result.failure(cancellation)
    }
}
