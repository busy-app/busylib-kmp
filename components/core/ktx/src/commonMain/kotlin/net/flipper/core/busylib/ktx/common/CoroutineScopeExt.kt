package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
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
