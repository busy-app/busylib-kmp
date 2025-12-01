package net.flipper.bridge.connection.feature.events.impl

import dev.dokky.bitvector.BitVector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BitVectorKtxTests {
    @Test
    fun `first bit true`() {
        val input = byteArrayOf(
            0b00000000.toByte(),
            0b00000000.toByte(),
            0b00000000.toByte(),
            0b00000001.toByte()
        )
        val bits = bitsOf(input)

        assertTrue(bits[0])
    }

    @Test
    fun `last bit true`() {
        val input = byteArrayOf(
            0b00000000.toByte(),
            0b00000000.toByte(),
            0b00000000.toByte(),
            0b10000000.toByte()
        )
        val bits = bitsOf(input)

        assertTrue(bits[7])
    }

    @Test
    fun `two bits true`() {
        val input = byteArrayOf(
            0b00000000.toByte(),
            0b00000000.toByte(),
            0b00000000.toByte(),
            0b10000001.toByte()
        )
        val bits = bitsOf(input)

        assertTrue(bits[0])
        assertTrue(bits[7])
    }


    @Test
    fun `two bytes first bit true`() {
        val input = byteArrayOf(
            0b00000000.toByte(),
            0b00000000.toByte(),
            0b00000001.toByte(),
            0b10000000.toByte()
        )
        val bits = bitsOf(input)

        assertTrue(bits[8])
    }

    @Test
    fun `two bytes two bit true`() {
        val input = byteArrayOf(
            0b00000000.toByte(),
            0b00000000.toByte(),
            0b00000001.toByte(),
            0b10000000.toByte()
        )
        val bits = bitsOf(input)

        assertTrue(bits[7])
        assertTrue(bits[8])
    }
}