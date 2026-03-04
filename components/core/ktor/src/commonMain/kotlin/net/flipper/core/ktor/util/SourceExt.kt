package net.flipper.core.ktor.util

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import net.flipper.core.busylib.log.error

@Suppress("LoopWithTooManyJumpStatements", "MagicNumber")
fun RawSource.asFlow(
    bufferSize: Long = 1 * 1024
): Flow<ByteArray> = flow {
    val source = this@asFlow.buffered()
    try {
        val buffer = Buffer()
        while (!source.exhausted() && currentCoroutineContext().isActive) {
            buffer.clear()
            val bytesRead = source.readAtMostTo(buffer, bufferSize)
            if (bytesRead == -1L) break
            if (bytesRead == 0L) continue
            emit(buffer.readByteArray())
        }
    } catch (t: Throwable) {
        error(t) { "#RawSource.asFlow Could not read from source" }
        throw t
    } finally {
        source.close()
    }
}
