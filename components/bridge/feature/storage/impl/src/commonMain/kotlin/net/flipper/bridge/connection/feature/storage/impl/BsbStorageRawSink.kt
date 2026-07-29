package net.flipper.bridge.connection.feature.storage.impl

import kotlinx.coroutines.runBlocking
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.RawSink
import kotlinx.io.readByteArray
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcStorageApi

/**
 * Writes a file onto the bar storage.
 *
 * The bar accepts whole files only, so nothing is sent until [flush] or
 * [close], and every upload carries the complete intended content:
 * [initialContent] plus everything written since. That makes repeated flushes
 * safe, and it is why appending has to start from the current content of the
 * file — the caller resolves that and passes it in.
 *
 * [close] uploads even when nothing was written, so opening and closing a sink
 * creates or truncates the file the way a local sink would.
 *
 * Memory use is proportional to the resulting file size. Not thread safe.
 */
internal class BsbStorageRawSink(
    private val devicePath: String,
    private val storageApi: FRpcStorageApi,
    initialContent: ByteArray
) : RawSink {
    private val content = Buffer().also { buffer -> buffer.write(initialContent) }
    private var isClosed = false
    private var hasUnsentContent = true

    private fun upload() {
        if (!hasUnsentContent) return
        // The buffer is copied, not consumed: a later write has to be able to
        // send the whole file again.
        val bytes = content.copy().readByteArray()
        runBlocking { storageApi.writeFile(devicePath, bytes) }
            .getOrElse { error ->
                throw IOException("Failed to write $devicePath to the bar storage", error)
            }
        hasUnsentContent = false
    }

    override fun write(source: Buffer, byteCount: Long) {
        require(byteCount >= 0) { "byteCount ($byteCount) must not be negative" }
        require(source.size >= byteCount) {
            "Source size (${source.size}) is below byteCount ($byteCount)"
        }
        check(!isClosed) { "Sink is closed" }
        content.write(source, byteCount)
        hasUnsentContent = true
    }

    override fun flush() {
        check(!isClosed) { "Sink is closed" }
        upload()
    }

    override fun close() {
        if (isClosed) return
        upload()
        isClosed = true
        content.clear()
    }
}
