package net.flipper.bridge.connection.transport.combined.impl.connections.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

@Suppress("FunctionName")
fun ChildSupervisorScope(
    parentScope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
    onCompletion: (Throwable?) -> Unit
) = run {
    // SupervisorJob without parent — complete isolation from parent hierarchy
    val job = SupervisorJob().apply {
        invokeOnCompletion { cause -> onCompletion.invoke(cause?.cause ?: cause) }
    }

    // Link cancellation: when parentScope is cancelled, our scope will be cancelled too.
    // A long-lived parent would otherwise keep one handler per child scope ever created.
    val parentCompletionHandle = parentScope.coroutineContext[Job]?.invokeOnCompletion {
        job.cancel()
    }
    job.invokeOnCompletion { parentCompletionHandle?.dispose() }

    // Catch unhandled exceptions and destroy the scope
    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        job.cancel(CancellationException("Child scope failed", throwable))
    }

    CoroutineScope(dispatcher + job + exceptionHandler)
}
