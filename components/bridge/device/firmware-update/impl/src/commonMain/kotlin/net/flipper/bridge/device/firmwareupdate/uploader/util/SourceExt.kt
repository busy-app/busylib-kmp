package net.flipper.bridge.device.firmwareupdate.uploader.util

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.readByteArray
import net.flipper.core.busylib.log.error

fun RawSource.asFlow(
    bufferSize: Long = 1 * 1024
): Flow<ByteArray> = flow {
    val buffer = Buffer()
    try {
        while (currentCoroutineContext().isActive) {
            val bytesRead = readAtMostTo(buffer, bufferSize)
            if (bytesRead == -1L || bytesRead <= 0L) break
            emit(buffer.readByteArray())
            yield()
        }
    } catch (t: Throwable) {
        error(t) { "#asFlow Could not read from source" }
        throw t
    } finally {
        close()
    }
}
