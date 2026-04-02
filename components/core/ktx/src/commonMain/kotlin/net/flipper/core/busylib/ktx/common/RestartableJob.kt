package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Represents a [Job], which can be restarted
 *
 * [RestartableJob] allows starting a new execution, cancelling the current one and awaiting its completion
 * If a job is already running, calling [start] is expected to skip its launch
 */
interface RestartableJob {

    /**
     * Indicates whether the job is currently active
     */
    val isActive: Boolean

    /**
     * Starts the job.
     *
     * If a job is already running, it should be skipped
     */
    fun start()

    /**
     * Cancels the current job without waiting for its completion
     */
    fun cancel()

    /**
     * Cancels the current job and suspends until its completion
     */
    suspend fun cancelAndJoin()

    /**
     * Suspends until the current job completes
     */
    suspend fun join()
}

class CoroutineRestartableJob(
    private val scope: CoroutineScope,
    private val block: suspend CoroutineScope.() -> Unit
) : RestartableJob {
    private var runnableJob: Job? = null
    private val mutex = Mutex()

    override val isActive: Boolean
        get() = runnableJob?.isActive == true

    override fun start() {
        scope.launch {
            mutex.withLock {
                if (runnableJob?.isActive == true) return@launch
                runnableJob?.cancelAndJoin()
                runnableJob = launch(block = block)
            }
        }
    }

    override suspend fun join() {
        runnableJob?.join()
    }

    override suspend fun cancelAndJoin() {
        mutex.withLock { runnableJob?.cancelAndJoin() }
    }

    override fun cancel() {
        scope.launch { cancelAndJoin() }
    }
}
