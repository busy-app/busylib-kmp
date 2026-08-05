package net.flipper.bridge.connection.feature.storage.impl

import kotlinx.coroutines.runBlocking
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.RawSource
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcStorageApi

/**
 * Reads a file off the bar storage.
 *
 * The bar cannot serve a byte range, so the first read downloads the whole file
 * and later reads come from memory: opening a source is cheap, the first read
 * is not, and the source holds the whole file until closed.
 *
 * Not thread safe.
 */
internal class BsbStorageRawSource(
    private val devicePath: String,
    private val storageApi: FRpcStorageApi
) : RawSource {
    private val content = Buffer()
    private var isLoaded = false
    private var isClosed = false

    private fun loadContent() {
        if (isLoaded) return
        val bytes = runBlocking { storageApi.readFile(devicePath) }
            .getOrElse { error ->
                throw IOException("Failed to read $devicePath from the bar storage", error)
            }
        content.write(bytes)
        isLoaded = true
    }

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        require(byteCount >= 0) { "byteCount ($byteCount) must not be negative" }
        check(!isClosed) { "Source is closed" }
        loadContent()
        return content.readAtMostTo(sink, byteCount)
    }

    override fun close() {
        isClosed = true
        content.clear()
    }
}
