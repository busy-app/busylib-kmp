package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.flipper.core.busylib.log.error

fun CoroutineScope.launchOnCompletion(block: suspend () -> Unit) {
    val parentContext = coroutineContext.minusKey(Job)
    val completionScope = CoroutineScope(SupervisorJob() + parentContext)
    val rootHandle = this.coroutineContext.job.invokeOnCompletion {
        completionScope.launch {
            try {
                block.invoke()
            } catch (t: Throwable) {
                error(t) { "#launchOnCompletion could not execute block" }
            } finally {
                completionScope.cancel()
            }
        }
    }
    completionScope.launch {
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                rootHandle.dispose()
            }
        }
    }
}
