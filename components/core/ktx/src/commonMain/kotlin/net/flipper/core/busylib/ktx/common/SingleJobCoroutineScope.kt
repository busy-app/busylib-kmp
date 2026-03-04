package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * A [CoroutineScope] that ensures only one coroutine job is active
 * at a time according to the selected [SingleJobMode]
 */
interface SingleJobCoroutineScope : CoroutineScope {
    /**
     * Launches a new coroutine within this scope, applying the given [mode]
     * to control how it interacts with any currently running job
     * @throws PreviousJobsRunningException if selected [SingleJobMode.SKIP_IF_RUNNING] and previous jobs still active
     */
    @DelicateSingleJobApi
    fun <T> async(
        mode: SingleJobMode,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T>

    /**
     * Launches a new coroutine within this scope, applying the given [mode]
     * to control how it interacts with any currently running job
     */
    fun launch(
        mode: SingleJobMode,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job
}

/**
 * Cancel previous running jobs as side effect
 */
fun SingleJobCoroutineScope.cancelPrevious(): Job {
    return launch(SingleJobMode.CANCEL_PREVIOUS, block = {}).job
}

@RequiresOptIn(
    message = "This is dangerous API of SingleJobCoroutineScope." +
        "See docs of methods, annotated with this",
    level = RequiresOptIn.Level.WARNING
)
annotation class DelicateSingleJobApi

class PreviousJobsRunningException(
    message: String = "Some of previous jobs were still running"
) : CancellationException(message)

/**
 * Defines how a [SingleJobCoroutineScope] behaves when a new coroutine launch
 * is requested while another job is already running
 */
enum class SingleJobMode {
    /**
     * Cancels any currently running job and immediately launches the new coroutine
     */
    CANCEL_PREVIOUS,

    /**
     * Waiting for the existing job to complete before launching the new coroutine
     */
    AWAIT_PREVIOUS,

    /**
     * If there is already an active job, skips launching a new coroutine
     * [SingleJobCoroutineScope.async] May throw exception if using this [SingleJobMode]
     * @see SingleJobCoroutineScope.async
     */
    SKIP_IF_RUNNING
}

private class MutexSingleJobCoroutineScope(
    private val scope: CoroutineScope
) : SingleJobCoroutineScope,
    CoroutineScope by scope {

    private val activeJobs = mutableListOf<Job>()
    private val mutex = Mutex()

    private fun <T> awaitPreviousUnsafe(
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T> {
        val previousJobs = activeJobs.toList()
        return async(
            context = context,
            start = start,
            block = {
                previousJobs.joinAll()
                block.invoke(this)
            }
        ).also(activeJobs::add)
    }

    private fun <T> cancelPreviousUnsafe(
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T> {
        activeJobs.forEach(Job::cancel)
        return async(
            context = context,
            start = start,
            block = block
        ).also(activeJobs::add)
    }

    private fun <T> trySkipPreviousUnsafe(
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend CoroutineScope.() -> T
    ): Result<Deferred<T>> {
        val isAnyJobActive = activeJobs.any(Job::isActive)
        return if (isAnyJobActive) {
            Result.failure(PreviousJobsRunningException())
        } else {
            Result.success(
                async(
                    context = context,
                    start = start,
                    block = block
                ).also(activeJobs::add)
            )
        }
    }

    override fun <T> async(
        mode: SingleJobMode,
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T> {
        return scope.async(
            context = context,
            start = start,
            block = {
                mutex.withLock {
                    when (mode) {
                        SingleJobMode.CANCEL_PREVIOUS -> {
                            cancelPreviousUnsafe(
                                context = context,
                                start = start,
                                block = block
                            )
                        }

                        SingleJobMode.AWAIT_PREVIOUS -> {
                            awaitPreviousUnsafe(
                                context = context,
                                start = start,
                                block = block
                            )
                        }

                        SingleJobMode.SKIP_IF_RUNNING -> {
                            trySkipPreviousUnsafe(
                                context = context,
                                start = start,
                                block = block
                            ).getOrThrow()
                        }
                    }
                }.await()
            }
        )
    }

    override fun launch(
        mode: SingleJobMode,
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return scope.async(
            context = context,
            start = start,
            block = {
                mutex.withLock {
                    when (mode) {
                        SingleJobMode.CANCEL_PREVIOUS -> {
                            cancelPreviousUnsafe(
                                context = context,
                                start = start,
                                block = block
                            )
                        }

                        SingleJobMode.AWAIT_PREVIOUS -> {
                            awaitPreviousUnsafe(
                                context = context,
                                start = start,
                                block = block
                            )
                        }

                        SingleJobMode.SKIP_IF_RUNNING -> {
                            trySkipPreviousUnsafe(
                                context = context,
                                start = start,
                                block = block
                            ).getOrNull()
                        }
                    }
                }?.join()
            }
        )
    }

    private fun listenForOnDestroy() {
        scope.coroutineContext[Job]?.invokeOnCompletion {
            runBlocking {
                mutex.withLock { activeJobs.clear() }
            }
        }
    }

    init {
        listenForOnDestroy()
    }
}

fun CoroutineScope.asSingleJobScope(): SingleJobCoroutineScope {
    return MutexSingleJobCoroutineScope(this)
}
