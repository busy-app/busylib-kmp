package com.flipperdevices.core.ktx.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
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
     * Launches a new coroutine within this scope, applying the given [mode]
     * to control how it interacts with any currently running job
     */
    fun launch(
        mode: SingleJobMode = SingleJobMode.SKIP_IF_RUNNING,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job
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
     */
    SKIP_IF_RUNNING
}

private class MutexSingleJobCoroutineScope(
    private val scope: CoroutineScope
) : SingleJobCoroutineScope,
    CoroutineScope by scope {

    private val activeJobs = mutableListOf<Job>()
    private val mutex = Mutex()

    override fun launch(
        mode: SingleJobMode,
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend CoroutineScope.() -> Unit
    ): Job = scope.launch(
        context = context,
        start = start,
        block = {
            mutex.withLock {
                activeJobs.removeAll(Job::isCompleted)
                when (mode) {
                    SingleJobMode.CANCEL_PREVIOUS -> {
                        activeJobs.forEach(Job::cancel)
                        val newJob = launch(
                            context = context,
                            start = start,
                            block = block
                        )
                        activeJobs.add(newJob)
                    }

                    SingleJobMode.AWAIT_PREVIOUS -> {
                        val previousJobs = activeJobs.toList()
                        val newJob = launch(
                            context = context,
                            start = start,
                            block = {
                                previousJobs.joinAll()
                                block.invoke(this)
                            }
                        )
                        activeJobs.add(newJob)
                    }

                    SingleJobMode.SKIP_IF_RUNNING -> {
                        val isAnyJobActive = activeJobs.any(Job::isActive)
                        if (isAnyJobActive) return@withLock
                        val newJob = launch(
                            context = context,
                            start = start,
                            block = block
                        )
                        activeJobs.add(newJob)
                    }
                }
            }
        }
    )

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
