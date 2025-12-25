package net.flipper.transport.ble.impl

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val len = length.toInt()
    val out = ByteArray(len)
    if (len == 0) return out

    out.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, len.toULong())
    }
    return out
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

fun ByteArray.chunked(size: Int): List<ByteArray> {
    require(size > 0) { "Chunk size must be greater than 0." }
    if (this.isEmpty()) return emptyList()
    if (this.size <= size) return listOf(this)
    val chunks = mutableListOf<ByteArray>()
    var offset = 0
    while (offset < this.size) {
        val length = minOf(size, this.size - offset)
        chunks.add(copyOfRange(offset, offset + length))
        offset += length
    }
    return chunks
}
