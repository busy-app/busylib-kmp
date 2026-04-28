package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.verbose

fun <T> LogTagProvider.wrapWebsocket(
    block: suspend () -> Flow<T>
) = flow<T> {
    var retryCount = 0
    while (currentCoroutineContext().isActive) {
        runSuspendCatching {
            // coroutineScope acts as a Job boundary so that exceptions from
            // internal `launch` coroutines (e.g. Ktor's OkHttp writeJob) are
            // rethrown here instead of bypassing runSuspendCatching and
            // crashing as uncaught on the thread pool.
            coroutineScope {
                info { "Subscribe to websocket" }
                block().catch {
                    retryCount++
                    error(it) { "Failed request websocket" }
                }.collect {
                    retryCount = 0
                    verbose { "Receive changes by websocket: $it" }
                    emit(it)
                }
            }
        }.onFailure { e ->
            retryCount++
            error(e) { "Failed request websocket" }
        }
        val delayTimeout = getExponentialDelay(retryCount)
        info { "Stop loop, wait ${delayTimeout.inWholeMilliseconds}ms" }
        delay(duration = delayTimeout)
    }
}
