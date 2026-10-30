package net.flipper.bridge.connection.transport.ble.impl.serial

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import net.flipper.bridge.connection.transport.ble.impl.toRequestCounter
import net.flipper.core.busylib.ktx.common.runSuspendCatchingUnlessCancelled
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import kotlin.time.Duration

internal fun LogTagProvider.pollRequestCounter(
    interval: Duration,
    readResetCharacteristic: suspend () -> ByteArray
): Flow<Int> = flow {
    while (currentCoroutineContext().isActive) {
        val payload = runSuspendCatchingUnlessCancelled { readResetCharacteristic.invoke() }
            .onFailure { throwable -> error(throwable) { "Failed to read request counter, keep polling" } }
            .getOrNull()
        if (payload != null) {
            val counter = payload.toRequestCounter()
            debug { "Receive request counter $counter" }
            emit(counter)
        }
        delay(interval)
    }
}
