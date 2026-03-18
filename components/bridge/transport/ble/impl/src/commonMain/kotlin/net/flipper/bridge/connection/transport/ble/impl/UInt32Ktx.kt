@file:Suppress("MagicNumber")

package net.flipper.bridge.connection.transport.ble.impl

private const val BYTE_MASK = 0xFF

fun ByteArray.toRequestCounter(): Int {
    if (size < Int.SIZE_BYTES) return 0
    return (this[0].toInt() and BYTE_MASK) or
        ((this[1].toInt() and BYTE_MASK) shl 8) or
        ((this[2].toInt() and BYTE_MASK) shl 16) or
        ((this[3].toInt() and BYTE_MASK) shl 24)
}

fun Int.toUInt32ByteArray(): ByteArray {
    return byteArrayOf(
        (this and BYTE_MASK).toByte(),
        ((this shr 8) and BYTE_MASK).toByte(),
        ((this shr 16) and BYTE_MASK).toByte(),
        ((this shr 24) and BYTE_MASK).toByte()
    )
}
