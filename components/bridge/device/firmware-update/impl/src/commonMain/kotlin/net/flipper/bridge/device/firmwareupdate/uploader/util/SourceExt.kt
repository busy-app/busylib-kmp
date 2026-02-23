package net.flipper.bridge.device.firmwareupdate.uploader.util

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import net.flipper.core.busylib.log.error
import okio.Buffer
import okio.Source

fun Source.asFlow(
    bufferSize: Long = 1 * 1024
): Flow<ByteArray> = flow {
    val buffer = Buffer()
    try {
        while (currentCoroutineContext().isActive) {
            val bytesRead = read(buffer, bufferSize)
            if (bytesRead == -1L || bytesRead <= 0L) break

            val bytes = buffer.readByteArray(bytesRead)
            emit(bytes)
            yield()
        }
    } catch (t: Throwable) {
        error(t) { "#asFlow Could not read from source" }
    } finally {
        close()
    }
}
