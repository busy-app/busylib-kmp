package net.flipper.core.busylib.ktx.common

fun ByteArray.chunked(size: Int): List<ByteArray> {
    require(size > 0) { "Chunk size must be greater than 0." }
    if (isEmpty()) return emptyList()
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
