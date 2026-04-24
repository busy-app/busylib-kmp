package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.flipper.core.busylib.log.error

fun CoroutineScope.launchOnCompletion(block: suspend () -> Unit) {
    launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                try {
                    block.invoke()
                } catch (t: Throwable) {
                    error(t) { "#launchOnCompletion 7 could not execute block" }
                }
            }
        }
    }
}

/**
 * Creates a new [CoroutineScope] that is cancelled when either of the given parent scopes is cancelled.
 *
 * Kotlin coroutines support only one real parent [Job], so this function does not create
 * a scope with two structured-concurrency parents. Instead, it creates an independent
 * child [Job] and manually links cancellation from both parent scopes to it.
 *
 * If [scopeFirst] or [scopeSecond] completes or is cancelled, the returned scope is cancelled as well.
 * Cancelling the returned scope does not cancel either parent scope.
 *
 * @param scopeFirst First parent-like scope whose completion cancels the returned scope.
 * @param scopeSecond Second parent-like scope whose completion cancels the returned scope.
 * @param dispatcher Dispatcher used by coroutines launched in the returned scope.
 *
 * @return A new [CoroutineScope] cancelled when either [scopeFirst] or [scopeSecond] completes.
 */
fun createLinkedScope(
    scopeFirst: CoroutineScope,
    scopeSecond: CoroutineScope,
    dispatcher: CoroutineDispatcher = FlipperDispatchers.default
): CoroutineScope {
    val childJob = SupervisorJob()
    val firstJob = requireNotNull(scopeFirst.coroutineContext[Job]) {
        "scopeFirst must contain a Job to link cancellation"
    }
    val secondJob = requireNotNull(scopeSecond.coroutineContext[Job]) {
        "scopeSecond must contain a Job to link cancellation"
    }

    val firstListener = firstJob.invokeOnCompletion { childJob.cancel() }
    val secondListener = secondJob.invokeOnCompletion {
        childJob.cancel()
    }
    childJob.invokeOnCompletion {
        firstListener.dispose()
        secondListener.dispose()
    }

    return CoroutineScope(childJob + dispatcher)
}
