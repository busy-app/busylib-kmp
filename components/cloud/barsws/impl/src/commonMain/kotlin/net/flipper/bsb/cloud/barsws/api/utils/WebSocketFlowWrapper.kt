package net.flipper.bsb.cloud.barsws.api.utils

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import net.flipper.core.busylib.ktx.common.getExponentialDelay
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info

fun <T> LogTagProvider.wrapWebsocket(
    block: suspend () -> Flow<T>
) = flow<T> {
    var retryCount = 0
    while (currentCoroutineContext().isActive) {
        runSuspendCatching {
            info { "Subscribe to websocket" }
            block().catch {
                retryCount++
                error(it) { "Failed request websocket" }
            }.collect {
                retryCount = 0
                info { "Receive changes by websocket: $it" }
                emit(it)
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
