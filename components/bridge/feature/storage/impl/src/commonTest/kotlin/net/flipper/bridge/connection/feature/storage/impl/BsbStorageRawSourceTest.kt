package net.flipper.bridge.connection.feature.storage.impl

import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BsbStorageRawSourceTest {
    private val filePath = "/ext/assets/frame001.png"
    private val fileContent = byteArrayOf(1, 2, 3, 4, 5)

    private val fake = FakeFRpcStorageApi()

    private fun createSource(): BsbStorageRawSource {
        return BsbStorageRawSource(devicePath = filePath, storageApi = fake)
    }

    /** Opening a source must stay cheap: the download is what costs. */
    @Test
    fun GIVEN_source_created_WHEN_nothing_is_read_THEN_bar_is_not_touched() {
        fake.putFile(filePath, fileContent)

        createSource()

        assertEquals(emptyList(), fake.requests)
    }

    @Test
    fun GIVEN_source_created_WHEN_closed_without_reading_THEN_bar_is_not_touched() {
        fake.putFile(filePath, fileContent)

        createSource().close()

        assertEquals(emptyList(), fake.requests)
    }

    @Test
    fun GIVEN_file_on_bar_WHEN_read_THEN_returns_file_content() {
        fake.putFile(filePath, fileContent)
        val sink = Buffer()

        val readCount = createSource().readAtMostTo(sink, Long.MAX_VALUE)

        assertEquals(fileContent.size.toLong(), readCount)
        assertContentEquals(fileContent, sink.readByteArray())
    }

    /**
     * The bar cannot serve a byte range, so the whole file is downloaded once
     * and later reads are served from memory.
     */
    @Test
    fun GIVEN_file_read_in_pieces_WHEN_read_repeatedly_THEN_bar_is_read_only_once() {
        fake.putFile(filePath, fileContent)
        val sink = Buffer()
        val source = createSource()

        repeat(fileContent.size) { source.readAtMostTo(sink, 1) }

        assertEquals(listOf("read $filePath"), fake.requests)
        assertContentEquals(fileContent, sink.readByteArray())
    }

    @Test
    fun GIVEN_byte_count_below_file_size_WHEN_read_THEN_returns_only_requested_bytes() {
        fake.putFile(filePath, fileContent)
        val sink = Buffer()

        val readCount = createSource().readAtMostTo(sink, 2)

        assertEquals(2L, readCount)
        assertContentEquals(byteArrayOf(1, 2), sink.readByteArray())
    }

    @Test
    fun GIVEN_exhausted_source_WHEN_read_again_THEN_returns_minus_one() {
        fake.putFile(filePath, fileContent)
        val sink = Buffer()
        val source = createSource()
        source.readAtMostTo(sink, Long.MAX_VALUE)

        assertEquals(-1L, source.readAtMostTo(sink, Long.MAX_VALUE))
    }

    /** An empty file has to report exhaustion, not stall the reader. */
    @Test
    fun GIVEN_empty_file_WHEN_read_THEN_returns_minus_one() {
        fake.putFile(filePath, ByteArray(0))

        assertEquals(-1L, createSource().readAtMostTo(Buffer(), Long.MAX_VALUE))
    }

    @Test
    fun GIVEN_zero_byte_count_WHEN_read_THEN_reads_nothing() {
        fake.putFile(filePath, fileContent)

        assertEquals(0L, createSource().readAtMostTo(Buffer(), 0))
    }

    @Test
    fun GIVEN_negative_byte_count_WHEN_read_THEN_throws_IllegalArgumentException() {
        fake.putFile(filePath, fileContent)

        assertFailsWith<IllegalArgumentException> { createSource().readAtMostTo(Buffer(), -1) }
    }

    @Test
    fun GIVEN_closed_source_WHEN_read_THEN_throws_IllegalStateException() {
        fake.putFile(filePath, fileContent)
        val source = createSource()
        source.close()

        assertFailsWith<IllegalStateException> { source.readAtMostTo(Buffer(), Long.MAX_VALUE) }
    }

    @Test
    fun GIVEN_source_WHEN_closed_twice_THEN_does_not_fail() {
        fake.putFile(filePath, fileContent)
        val source = createSource()

        source.close()
        source.close()
    }

    @Test
    fun GIVEN_missing_file_WHEN_read_THEN_throws_IOException_keeping_the_reason() {
        val error = assertFailsWith<IOException> {
            createSource().readAtMostTo(Buffer(), Long.MAX_VALUE)
        }

        assertTrue(error.message.orEmpty().contains(filePath))
        assertTrue(error.cause != null)
    }

    /**
     * A failed download must not poison the source: the bar refusing once is
     * routinely a transient connection problem, and the caller may retry.
     */
    @Test
    fun GIVEN_read_failed_WHEN_read_again_after_bar_recovers_THEN_returns_file_content() {
        fake.putFile(filePath, fileContent)
        fake.refusedRequests += "read $filePath"
        val source = createSource()
        assertFailsWith<IOException> { source.readAtMostTo(Buffer(), Long.MAX_VALUE) }
        fake.refusedRequests.clear()
        val sink = Buffer()

        source.readAtMostTo(sink, Long.MAX_VALUE)

        assertContentEquals(fileContent, sink.readByteArray())
        assertEquals(2, fake.requestsOf("read").size)
    }
}
