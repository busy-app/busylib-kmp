package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

fun CoroutineScope.launchOnCompletion(block: suspend () -> Unit) {
    val parentContext = coroutineContext.minusKey(Job)
    val completionScope = CoroutineScope(SupervisorJob() + parentContext)
    this.coroutineContext.job.invokeOnCompletion {
        completionScope.launch { block.invoke() }
            .invokeOnCompletion { completionScope.cancel() }
    }
}
