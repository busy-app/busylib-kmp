package net.flipper.bridge.connection.feature.storage.impl

import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BsbStorageRawSinkTest {
    private val filePath = "/ext/assets/frame001.png"

    private val fake = FakeFRpcStorageApi()

    private fun createSink(initialContent: ByteArray = ByteArray(0)): BsbStorageRawSink {
        fake.putDirectory("/ext/assets")
        return BsbStorageRawSink(
            devicePath = filePath,
            storageApi = fake,
            initialContent = initialContent
        )
    }

    private fun bufferOf(vararg bytes: Byte): Buffer {
        return Buffer().also { buffer -> buffer.write(bytes) }
    }

    /** Nothing may travel before the caller asks for it. */
    @Test
    fun GIVEN_sink_created_WHEN_bytes_written_but_not_flushed_THEN_bar_is_not_touched() {
        val sink = createSink()

        sink.write(bufferOf(1, 2, 3), 3)

        assertEquals(emptyList(), fake.requests)
    }

    /**
     * A local sink creates or truncates its file on open, so closing without a
     * single write has to leave an empty file behind rather than nothing.
     */
    @Test
    fun GIVEN_sink_created_WHEN_closed_without_writes_THEN_empty_file_is_written() {
        createSink().close()

        assertContentEquals(ByteArray(0), fake.fileContentOrNull(filePath))
    }

    @Test
    fun GIVEN_bytes_written_WHEN_flushed_THEN_file_holds_them() {
        val sink = createSink()

        sink.write(bufferOf(1, 2, 3), 3)
        sink.flush()

        assertContentEquals(byteArrayOf(1, 2, 3), fake.fileContentOrNull(filePath))
    }

    @Test
    fun GIVEN_bytes_written_in_pieces_WHEN_closed_THEN_file_holds_them_in_order() {
        val sink = createSink()

        sink.write(bufferOf(1, 2), 2)
        sink.write(bufferOf(3), 1)
        sink.close()

        assertContentEquals(byteArrayOf(1, 2, 3), fake.fileContentOrNull(filePath))
    }

    /** Appending is emulated, so what came before must lead what is written. */
    @Test
    fun GIVEN_initial_content_WHEN_bytes_written_and_closed_THEN_file_holds_initial_then_written() {
        val sink = createSink(initialContent = byteArrayOf(7, 8))

        sink.write(bufferOf(1, 2), 2)
        sink.close()

        assertContentEquals(byteArrayOf(7, 8, 1, 2), fake.fileContentOrNull(filePath))
    }

    @Test
    fun GIVEN_initial_content_WHEN_closed_without_writes_THEN_file_holds_initial_content() {
        createSink(initialContent = byteArrayOf(7, 8)).close()

        assertContentEquals(byteArrayOf(7, 8), fake.fileContentOrNull(filePath))
    }

    @Test
    fun GIVEN_byte_count_below_source_size_WHEN_written_THEN_only_requested_bytes_are_sent() {
        val sink = createSink()

        sink.write(bufferOf(1, 2, 3, 4, 5), 2)
        sink.close()

        assertContentEquals(byteArrayOf(1, 2), fake.fileContentOrNull(filePath))
    }

    /**
     * Every upload carries the complete file, so a second one with nothing new
     * to say would only repeat the first.
     */
    @Test
    fun GIVEN_flushed_sink_WHEN_flushed_again_without_writes_THEN_bar_is_written_once() {
        val sink = createSink()
        sink.write(bufferOf(1, 2), 2)
        sink.flush()

        sink.flush()

        assertEquals(1, fake.requestsOf("write").size)
    }

    @Test
    fun GIVEN_flushed_sink_WHEN_closed_without_writes_THEN_bar_is_written_once() {
        val sink = createSink()
        sink.write(bufferOf(1, 2), 2)
        sink.flush()

        sink.close()

        assertEquals(1, fake.requestsOf("write").size)
    }

    /**
     * The pending content is copied rather than consumed when uploading. Were
     * it consumed, a flush in the middle of writing would truncate the file to
     * whatever came after it.
     */
    @Test
    fun GIVEN_flushed_sink_WHEN_more_bytes_written_and_closed_THEN_file_holds_everything() {
        val sink = createSink(initialContent = byteArrayOf(9))
        sink.write(bufferOf(1, 2), 2)
        sink.flush()

        sink.write(bufferOf(3, 4), 2)
        sink.close()

        assertEquals(2, fake.requestsOf("write").size)
        assertContentEquals(byteArrayOf(9, 1, 2, 3, 4), fake.fileContentOrNull(filePath))
    }

    @Test
    fun GIVEN_sink_WHEN_closed_twice_THEN_bar_is_written_once() {
        val sink = createSink()

        sink.close()
        sink.close()

        assertEquals(1, fake.requestsOf("write").size)
    }

    @Test
    fun GIVEN_closed_sink_WHEN_bytes_written_THEN_throws_IllegalStateException() {
        val sink = createSink()
        sink.close()

        assertFailsWith<IllegalStateException> { sink.write(bufferOf(1), 1) }
    }

    @Test
    fun GIVEN_closed_sink_WHEN_flushed_THEN_throws_IllegalStateException() {
        val sink = createSink()
        sink.close()

        assertFailsWith<IllegalStateException> { sink.flush() }
    }

    @Test
    fun GIVEN_byte_count_above_source_size_WHEN_written_THEN_throws_IllegalArgumentException() {
        val sink = createSink()

        assertFailsWith<IllegalArgumentException> { sink.write(bufferOf(1, 2), 3) }
    }

    @Test
    fun GIVEN_negative_byte_count_WHEN_written_THEN_throws_IllegalArgumentException() {
        val sink = createSink()

        assertFailsWith<IllegalArgumentException> { sink.write(bufferOf(1, 2), -1) }
    }

    @Test
    fun GIVEN_refusing_bar_WHEN_flushed_THEN_throws_IOException_keeping_the_reason() {
        val sink = createSink()
        fake.refusedRequests += "write $filePath"
        sink.write(bufferOf(1), 1)

        val error = assertFailsWith<IOException> { sink.flush() }

        assertTrue(error.message.orEmpty().contains(filePath))
        assertTrue(error.cause != null)
    }

    @Test
    fun GIVEN_refusing_bar_WHEN_closed_THEN_throws_IOException() {
        val sink = createSink()
        fake.refusedRequests += "write $filePath"

        assertFailsWith<IOException> { sink.close() }
    }

    /**
     * A refused flush must not count as sent, or the content it carried would
     * never reach the bar even though the caller closes the sink afterwards.
     */
    @Test
    fun GIVEN_flush_refused_WHEN_closed_after_bar_recovers_THEN_file_holds_the_content() {
        val sink = createSink()
        fake.refusedRequests += "write $filePath"
        sink.write(bufferOf(1, 2), 2)
        assertFailsWith<IOException> { sink.flush() }
        fake.refusedRequests.clear()

        sink.close()

        assertContentEquals(byteArrayOf(1, 2), fake.fileContentOrNull(filePath))
    }
}
