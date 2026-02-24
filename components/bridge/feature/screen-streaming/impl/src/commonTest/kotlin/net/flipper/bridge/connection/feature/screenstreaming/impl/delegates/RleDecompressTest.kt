package net.flipper.bridge.connection.feature.screenstreaming.impl.delegates

import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RleDecompressTest {
    @Test
    fun GIVEN_repeated_block_WHEN_decompress_THEN_block_repeated() {
        // Control byte 0x03 = repeat next block 3 times
        val input = byteArrayOf(0x03, 0x0A, 0x0B, 0x0C)
        val expected = byteArrayOf(
            0x0A, 0x0B, 0x0C,
            0x0A, 0x0B, 0x0C,
            0x0A, 0x0B, 0x0C
        )

        val result = rleDecompress(input, blkSize = 3)

        assertContentEquals(expected, result)
    }

    @Test
    fun GIVEN_unique_blocks_WHEN_decompress_THEN_blocks_copied() {
        // Control byte 0x82 = 0x80 | 2 => 2 unique blocks follow
        val input = byteArrayOf(
            0x82.toByte(),
            0x0A,
            0x0B,
            0x0C,
            0x0D,
            0x0E,
            0x0F
        )
        val expected = byteArrayOf(
            0x0A,
            0x0B,
            0x0C,
            0x0D,
            0x0E,
            0x0F
        )

        val result = rleDecompress(input, blkSize = 3)

        assertContentEquals(expected, result)
    }

    @Test
    fun GIVEN_mixed_repeated_and_unique_WHEN_decompress_THEN_correct_output() {
        // First: repeat block [AA, BB, CC] x2
        // Then: 1 unique block [DD, EE, FF]
        val input = byteArrayOf(
            0x02,
            0xAA.toByte(),
            0xBB.toByte(),
            0xCC.toByte(),
            0x81.toByte(),
            0xDD.toByte(),
            0xEE.toByte(),
            0xFF.toByte()
        )
        val expected = byteArrayOf(
            0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(),
            0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(),
            0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte()
        )

        val result = rleDecompress(input, blkSize = 3)

        assertContentEquals(expected, result)
    }

    @Test
    fun GIVEN_empty_input_WHEN_decompress_THEN_empty_output() {
        val result = rleDecompress(byteArrayOf(), blkSize = 3)

        assertTrue(result.isEmpty())
    }

    @Test
    fun GIVEN_single_repeat_WHEN_decompress_THEN_single_block() {
        // Control byte 0x01 = repeat next block 1 time
        val input = byteArrayOf(0x01, 0x11, 0x22, 0x33)
        val expected = byteArrayOf(0x11, 0x22, 0x33)

        val result = rleDecompress(input, blkSize = 3)

        assertContentEquals(expected, result)
    }

    @Test
    fun GIVEN_blkSize_1_WHEN_decompress_THEN_correct_output() {
        // Repeat byte [0x42] x4, then 2 unique bytes
        val input = byteArrayOf(
            0x04,
            0x42,
            0x82.toByte(),
            0x0A,
            0x0B
        )
        val expected = byteArrayOf(
            0x42,
            0x42,
            0x42,
            0x42,
            0x0A,
            0x0B
        )

        val result = rleDecompress(input, blkSize = 1)

        assertContentEquals(expected, result)
    }

    @Test
    fun GIVEN_test_frame_base64_WHEN_decompress_THEN_valid_output() {
        val testFrameBase64 = "fwAAACIAAACLQzovmIFl4caemIFlQzovAAAABBMxUpXlM4fkMXbhBBMx" +
            "PQAAAIuYgWXhxp7hxp7hxp6YgWUAAABjoucWXd1fsOwWXd0QRJ09AAAABeHGnoYAAAA7ku" +
            "VfsOyy2vVfsOwWXd0FAAAAlP///wAAAAAAAAAAAP///////////////wAAAAAAAP//////" +
            "/////////wAAAAAAAAAAAP///////////yQAAACLmIFl4cae4cae4caemIFlAAAAOHziFl" +
            "3dX7DsFl3dEESdBQAAAJX///8AAAAAAAAAAAD///8AAAAAAAAAAAD///8AAAD///8AAAAA" +
            "AAAAAAD///8AAAD///8AAAAAAAAAAAD///8jAAAAoUM6L5iBZeHGnpiBZUM6LwAAAAQTMR" +
            "BEnRZd3RBEnQQTMQAAAAAAAAAAAAAAAP///wAAAP///wAAAAAAAP///wAAAAAAAAAAAP//" +
            "/wAAAP///wAAAAAAAAAAAP///wAAAP///zYAAACV////AAAA////AAAAAAAA//////////" +
            "//////AAAAAAAA////////////////AAAAAAAAAAAA////////////JAAAAJVDOi+YgWXh" +
            "xp6YgWVDOi8AAABDOi+YgWXhxp6YgWVDOi8AAAAAAAAAAAAAAAD///////////8AAAAAAA" +
            "D///8FAAAAgf///wkAAACB////IwAAAJWYgWXhxp7hxp7hxp6YgWUAAACYgWXhxp7hxp7h" +
            "xp6YgWUAAAAAAAAAAAD///8AAAAAAAAAAAD///8AAAD///8FAAAAgf///wUAAACF////AA" +
            "AAAAAAAAAA////IwAAAAXhxp6BAAAABeHGngMAAACH////AAAAAAAAAAAA////AAAA////" +
            "BQAAAIH///8GAAAAA////yQAAACLmIFl4cae4cae4caemIFlAAAAmIFl4cae4cae4caemI" +
            "FlPQAAAItDOi+YgWXhxp6YgWVDOi8AAABDOi+YgWXhxp6YgWVDOi9/AAAAfwAAAAYAAAA="

        val compressed = Base64.decode(testFrameBase64)
        val decompressed = rleDecompress(compressed, blkSize = 3)

        // Output must be non-empty
        assertTrue(decompressed.isNotEmpty())

        // Output size must be a multiple of blkSize
        assertEquals(0, decompressed.size % 3)

        // Decompression is deterministic
        val decompressed2 = rleDecompress(compressed, blkSize = 3)
        assertContentEquals(decompressed, decompressed2)
    }
}
