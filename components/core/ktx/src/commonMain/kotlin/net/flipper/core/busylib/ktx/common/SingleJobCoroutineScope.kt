package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.CancellationException
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

/**
 * A [CoroutineScope] that ensures only one coroutine job is active
 * at a time according to the selected [SingleJobMode]
 */
interface SingleJobCoroutineScope : CoroutineScope {

    /**
     * Launches a new coroutine within this scope, applying the given [mode]
     * to control how it interacts with any currently running job
     * @throws CancellationException if selected [SingleJobMode.SKIP_IF_RUNNING]
     */
    fun <T> withJobMode(
        mode: SingleJobMode,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T>
}

@RequiresOptIn("This is internal API of the Compose gradle plugin.")
annotation class DelicateSingleJobApi

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
     */
    @DelicateSingleJobApi
    SKIP_IF_RUNNING
}

private class MutexSingleJobCoroutineScope(
    private val scope: CoroutineScope
) : SingleJobCoroutineScope,
    CoroutineScope by scope {

    private val activeJobs = mutableListOf<Job>()
    private val mutex = Mutex()

    override fun <T> withJobMode(
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
                            activeJobs.forEach(Job::cancel)
                            async(
                                context = context,
                                start = start,
                                block = block
                            ).also(activeJobs::add)
                        }

                        SingleJobMode.AWAIT_PREVIOUS -> {
                            val previousJobs = activeJobs.toList()
                            async(
                                context = context,
                                start = start,
                                block = {
                                    previousJobs.joinAll()
                                    block.invoke(this)
                                }
                            ).also(activeJobs::add)
                        }

                        @OptIn(DelicateSingleJobApi::class)
                        SingleJobMode.SKIP_IF_RUNNING
                        -> {
                            val isAnyJobActive = activeJobs.any(Job::isActive)
                            if (isAnyJobActive) {
                                throw CancellationException(
                                    "A new coroutine cannot be launched while a previous one is running"
                                )
                            } else {
                                async(
                                    context = context,
                                    start = start,
                                    block = block
                                ).also(activeJobs::add)
                            }
                        }
                    }
                }.await()
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

fun SingleJobCoroutineScope.cancelPrevious(): Job {
    return withJobMode(SingleJobMode.CANCEL_PREVIOUS, block = {}).job
}

fun CoroutineScope.asSingleJobScope(): SingleJobCoroutineScope {
    return MutexSingleJobCoroutineScope(this)
}
