package net.flipper.bridge.connection.feature.screenstreaming.impl.delegates

private const val INITIAL_CAPACITY = 1024

private fun ByteArray.grownFor(size: Int, required: Int): ByteArray {
    if (required <= this.size) return this
    var capacity = maxOf(this.size, INITIAL_CAPACITY)
    while (capacity < required) {
        capacity *= 2
    }
    return copyInto(ByteArray(capacity), endIndex = size)
}

/**
 * Decompresses run-length encoded data.
 *
 * Control byte format:
 * - High bit set (0x80): unique blocks follow, lower 7 bits = count of unique blocks
 * - High bit clear: repeated block, byte value = repeat count
 */
@Suppress("MagicNumber")
fun rleDecompress(data: ByteArray, blkSize: Int): ByteArray {
    var index = 0
    val dataLen = data.size
    var decompressed = ByteArray(0)
    var size = 0

    while (index < dataLen) {
        val ctrlByte = data[index].toInt() and 0xFF
        index++

        if ((ctrlByte and 0x80) != 0) {
            // Unique blocks: ctrl_byte & 0x7F = unique sequence length
            val byteCount = (ctrlByte and 0x7F) * blkSize
            decompressed = decompressed.grownFor(size, size + byteCount)
            data.copyInto(
                destination = decompressed,
                destinationOffset = size,
                startIndex = index,
                endIndex = index + byteCount
            )
            size += byteCount
            index += byteCount
        } else {
            // Repeated block: ctrl_byte = repeat count
            val count = ctrlByte
            decompressed = decompressed.grownFor(size, size + count * blkSize)
            repeat(count) {
                data.copyInto(
                    destination = decompressed,
                    destinationOffset = size,
                    startIndex = index,
                    endIndex = index + blkSize
                )
                size += blkSize
            }
            index += blkSize
        }
    }

    return decompressed.copyOf(size)
}
