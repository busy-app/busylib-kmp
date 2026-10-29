package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import net.flipper.busylib.kmp.components.core.buildkonfig.BuildKonfig
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.verbose
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private fun TimeMark?.hasElapsedAtLeast(duration: Duration): Boolean {
    return this != null && elapsedNow() >= duration
}

fun <T> LogTagProvider.wrapWebsocket(
    maxDelay: Duration = 10.seconds,
    healthyAfter: Duration = Duration.ZERO,
    timeSource: TimeSource = TimeSource.Monotonic,
    block: suspend () -> Flow<T>
) = flow<T> {
    var retryCount = 0
    while (currentCoroutineContext().isActive) {
        var firstEmissionMark: TimeMark? = null
        runSuspendCatching {
            // coroutineScope acts as a Job boundary so that exceptions from
            // internal `launch` coroutines (e.g. Ktor's OkHttp writeJob) are
            // rethrown here instead of bypassing runSuspendCatching and
            // crashing as uncaught on the thread pool.
            coroutineScope {
                info { "Subscribe to websocket" }
                block().catch {
                    error(it) { "Failed request websocket" }
                }.collect {
                    if (firstEmissionMark == null) {
                        firstEmissionMark = timeSource.markNow()
                    }
                    if (BuildKonfig.IS_EVENT_SPAM_LOG_ENABLED) {
                        verbose { "Receive changes by websocket: $it" }
                    }
                    emit(it)
                }
            }
        }.onFailure { e ->
            error(e) { "Failed request websocket" }
        }
        retryCount = if (firstEmissionMark.hasElapsedAtLeast(healthyAfter)) 0 else retryCount + 1
        val delayTimeout = getExponentialDelay(retryCount, maxDelay = maxDelay)
        info { "Stop loop, wait ${delayTimeout.inWholeMilliseconds}ms" }
        delay(duration = delayTimeout)
    }
}
