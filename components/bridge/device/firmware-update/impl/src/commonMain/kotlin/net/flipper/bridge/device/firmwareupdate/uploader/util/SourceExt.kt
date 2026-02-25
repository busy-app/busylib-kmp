package net.flipper.bridge.device.firmwareupdate.uploader.util

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info

fun RawSource.asFlow(
    bufferSize: Long = 1 * 1024
): Flow<ByteArray> = flow {
    val buffer = Buffer()

    try {
        while (currentCoroutineContext().isActive) {
            buffer.clear()
            val bytesRead = readAtMostTo(buffer, bufferSize)
            if (bytesRead == -1L) break
            else if (bytesRead == 0L) {
                error { "#RawSource.asFlow received invalid bytesRead: $bytesRead" }
                break
            } else {
                emit(buffer.readByteArray())
                yield()
            }
        }
        info { "#RawSource.asFlow successfully read all source" }
    } catch (t: Throwable) {
        error(t) { "#RawSource.asFlow Could not read from source" }
        throw t
    } finally {
        close()
    }
}
