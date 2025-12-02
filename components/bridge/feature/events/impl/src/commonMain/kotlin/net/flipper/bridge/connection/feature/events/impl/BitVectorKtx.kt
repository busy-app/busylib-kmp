package net.flipper.bridge.connection.feature.events.impl

import dev.dokky.bitvector.BitVector
import dev.dokky.bitvector.MutableBitVector


/**
 * Parse uint32_t little-endian array to [BitVector]
 *
 *
 */
fun bitsOf(byteArray: ByteArray): BitVector {
    val vector = MutableBitVector()

    byteArray.toList()
        .chunked(UInt.SIZE_BYTES) // Just in case, if there are more than 32 bits
        .map { uintBytes ->
            uintBytes
                .map { byte ->
                    byte
                        .toBits()
                        .toList()
                        .reversed() // Because little-endian
                }.flatten()
        }.flatten()
        .forEachIndexed { index, bool ->
            vector[index] = bool
        }

    return vector
}

// For debug:

private fun Byte.toBits(): List<Boolean> {
    return ((Byte.SIZE_BITS - 1) downTo 0).map { bitIndex ->
        ((this.toInt() shr bitIndex) and 1) == 1
    }
}

private fun Byte.toBitsString(): String {
    return "0b" + toBits().map { if (it) 1 else 0 }.joinToString("")
}

fun ByteArray.toBitsString(): String {
    return "[" + joinToString(", ") { it.toBitsString() } + "]"
}