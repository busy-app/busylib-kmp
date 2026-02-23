package net.flipper.core.ktor.util

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import net.flipper.core.busylib.log.error

fun ByteReadChannel.asFlow(
    bufferSize: Int = 1 * 1024
): Flow<ByteArray> = flow {
    val buffer = ByteArray(bufferSize)
    try {
        while (!isClosedForRead && currentCoroutineContext().isActive) {
            awaitContent()
            val bytesRead = readAvailable(buffer)
            if (bytesRead == -1 || bytesRead <= 0) {
                error { "#asFlow received invalid bytesRead: $bytesRead" }
                break
            }
            emit(buffer.copyOf(bytesRead))
            yield()
        }
    } catch (t: Throwable) {
        error(t) { "#asFlow error" }
    }
}
