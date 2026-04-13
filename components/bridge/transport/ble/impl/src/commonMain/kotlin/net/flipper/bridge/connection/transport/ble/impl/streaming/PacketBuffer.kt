package net.flipper.bridge.connection.transport.ble.impl.streaming

internal class PacketBuffer {
    private val chunks = mutableListOf<ByteArray>()
    private var totalSize = 0

    fun hasData(): Boolean = totalSize > 0

    fun append(chunk: ByteArray) {
        if (chunks.isEmpty()) return
        chunks += chunk.copyOf()
        totalSize += chunk.size
    }

    fun drain(): ByteArray? {
        if (totalSize == 0) return null

        val packet = ByteArray(totalSize)
        var offset = 0
        chunks.forEach { chunk ->
            chunk.copyInto(packet, destinationOffset = offset)
            offset += chunk.size
        }

        chunks.clear()
        totalSize = 0
        return packet
    }
}
