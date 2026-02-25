package net.flipper.core.ktor.util

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ClosedByteChannelException
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
            val bytesRead = readAvailable(buffer)
            if (bytesRead == 0) {
                error { "#ByteReadChannel.asFlow received invalid bytesRead: $bytesRead" }
                awaitContent()
            } else if (bytesRead == -1) {
                break
            } else {
                emit(buffer.copyOf(bytesRead))
                yield()
            }
        }
    } catch (_: ClosedByteChannelException) {
    } catch (t: Throwable) {
        error(t) { "#ByteReadChannel.asFlow unhandled error" }
        throw t
    }
}
