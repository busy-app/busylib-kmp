package net.flipper.bridge.connection.transport.ble.impl.streaming

private const val HEADER_SIZE = 6
class BleStatusStreamingPacketParser {
    private val chunks = mutableListOf<ByteArray>()
    private var expectedCount: Int? = null
    private var expectedNextNum = 0
    private var totalPayloadSize = 0

    private data class Packet(
        val num: Int,
        val count: Int,
        val size: Int
    )

    fun consume(chunk: ByteArray): ByteArray? {
        val header = chunk.parseHeaderOrNull()
        if (header == null) {
            reset()
            return null
        }

        val isStartChunk = header.num == 0
        val hasActiveAssembly = expectedCount != null

        if (!hasActiveAssembly) {
            if (!isStartChunk) return null
            expectedCount = header.count
            expectedNextNum = 0
        } else if (header.count != expectedCount || header.num != expectedNextNum) {
            reset()
            if (!isStartChunk) return null

            expectedCount = header.count
            expectedNextNum = 0
        }

        val payloadChunk = chunk.copyOfRange(HEADER_SIZE, HEADER_SIZE + header.size)
        chunks.add(payloadChunk)
        totalPayloadSize += payloadChunk.size
        expectedNextNum += 1

        if (header.num != header.count - 1) return null

        return assemblePayload().also { reset() }
    }

    private fun reset() {
        chunks.clear()
        expectedCount = null
        expectedNextNum = 0
        totalPayloadSize = 0
    }

    private fun assemblePayload(): ByteArray {
        val payload = ByteArray(totalPayloadSize)
        var offset = 0
        chunks.forEach { bytes ->
            bytes.copyInto(payload, destinationOffset = offset)
            offset += bytes.size
        }
        return payload
    }

    @Suppress("MagicNumber")
    private fun ByteArray.parseHeaderOrNull(): Packet? {
        if (size < HEADER_SIZE) return null

        val num = getUShortLE(0)
        val count = getUShortLE(2)
        val payloadSize = getUShortLE(4)

        if (count == 0 || num >= count || payloadSize > size - HEADER_SIZE) return null

        return Packet(
            num = num,
            count = count,
            size = payloadSize
        )
    }

    @Suppress("MagicNumber")
    private fun ByteArray.getUShortLE(offset: Int): Int {
        return (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)
    }
}
