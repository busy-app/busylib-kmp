package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A [CoroutineScope] that ensures only one coroutine job is active
 * at a time according to the selected [SingleJobMode]
 */
interface SingleJobCoroutineScope : CoroutineScope {
    /**
     * Launches a new coroutine within this scope, applying the [SingleJobMode.CANCEL_PREVIOUS] mode
     */
    fun <T> async(
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
        scope: CoroutineScope,
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T> {
        val previousJobs = activeJobs.toList()
        return scope.async(
            context = context,
            start = start,
            block = {
                previousJobs.joinAll()
                block.invoke(this)
            }
        ).also(activeJobs::add)
    }

    private fun <T> cancelPreviousUnsafe(
        scope: CoroutineScope,
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T> {
        activeJobs.forEach(Job::cancel)
        return scope.async(
            context = context,
            start = start,
            block = block
        ).also(activeJobs::add)
    }

    private fun <T> trySkipPreviousUnsafe(
        scope: CoroutineScope,
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T>? {
        val isAnyJobActive = activeJobs.any(Job::isActive)
        return if (isAnyJobActive) {
            null
        } else {
            scope.async(
                context = context,
                start = start,
                block = block
            ).also(activeJobs::add)
        }
    }

    override fun <T> async(
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T> {
        return scope.async(
            context = context,
            start = start,
            block = {
                mutex.withLock {
                    cancelPreviousUnsafe(
                        context = context,
                        start = start,
                        block = block,
                        scope = this
                    )
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
                                block = block,
                                scope = this
                            )
                        }

                        SingleJobMode.AWAIT_PREVIOUS -> {
                            awaitPreviousUnsafe(
                                context = context,
                                start = start,
                                block = block,
                                scope = this
                            )
                        }

                        SingleJobMode.SKIP_IF_RUNNING -> {
                            trySkipPreviousUnsafe(
                                context = context,
                                start = start,
                                block = block,
                                scope = this
                            )
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

fun <T> Flow<T>.launchIn(
    scope: SingleJobCoroutineScope,
    mode: SingleJobMode = SingleJobMode.CANCEL_PREVIOUS
): Job = scope.launch(mode) { collect() }
