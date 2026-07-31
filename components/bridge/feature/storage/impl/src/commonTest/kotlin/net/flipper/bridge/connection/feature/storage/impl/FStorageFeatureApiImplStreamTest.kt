package net.flipper.bridge.connection.feature.storage.impl

import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FStorageFeatureApiImplStreamTest {
    private val fake = FakeFRpcStorageApi()

    private val api = FStorageFeatureApiImpl(
        storageApi = fake,
        pathResolver = BsbStoragePathResolver()
    )

    private fun bufferOf(vararg bytes: Byte): Buffer {
        return Buffer().also { buffer -> buffer.write(bytes) }
    }

    @Test
    fun GIVEN_existing_file_WHEN_source_read_THEN_returns_the_file_content() = runTest {
        fake.putFile("/ext/assets/a.png", byteArrayOf(1, 2, 3))

        val content = api.source(Path("/ext/assets/a.png")).buffered().use { source ->
            source.readByteArray()
        }

        assertContentEquals(byteArrayOf(1, 2, 3), content)
    }

    /**
     * Opening a source only names the file; whether it can be read shows up
     * when it is read, which is what keeps opening one cheap.
     */
    @Test
    fun GIVEN_missing_file_WHEN_source_opened_THEN_nothing_fails_until_it_is_read() = runTest {
        fake.putDirectory("/ext/assets")

        val source = api.source(Path("/ext/assets/a.png"))

        assertEquals(emptyList(), fake.requests)
        assertFailsWith<IOException> { source.readAtMostTo(Buffer(), Long.MAX_VALUE) }
    }

    @Test
    fun GIVEN_invalid_path_WHEN_source_opened_THEN_throws_IOException() = runTest {
        assertFailsWith<IOException> { api.source(Path("/etc/passwd")) }
    }

    @Test
    fun GIVEN_overwriting_sink_WHEN_bytes_written_and_closed_THEN_file_is_replaced() = runTest {
        fake.putFile("/ext/assets/a.png", byteArrayOf(9, 9, 9, 9))

        api.sink(Path("/ext/assets/a.png")).use { sink ->
            sink.write(bufferOf(1, 2), 2)
        }

        assertContentEquals(byteArrayOf(1, 2), fake.fileContentOrNull("/ext/assets/a.png"))
    }

    /** Overwriting has no use for what is already there, so it is not fetched. */
    @Test
    fun GIVEN_overwriting_sink_WHEN_opened_over_existing_file_THEN_content_is_not_downloaded() = runTest {
        fake.putFile("/ext/assets/a.png", byteArrayOf(9))

        api.sink(Path("/ext/assets/a.png"), append = false)

        assertEquals(emptyList(), fake.requestsOf("read"))
    }

    /**
     * The bar cannot append, so appending is a download, a concatenation and a
     * full re-upload.
     */
    @Test
    fun GIVEN_appending_sink_WHEN_bytes_written_and_closed_THEN_content_is_appended() = runTest {
        fake.putFile("/ext/assets/a.png", byteArrayOf(7, 8))

        api.sink(Path("/ext/assets/a.png"), append = true).use { sink ->
            sink.write(bufferOf(1, 2), 2)
        }

        assertContentEquals(
            byteArrayOf(7, 8, 1, 2),
            fake.fileContentOrNull("/ext/assets/a.png")
        )
    }

    @Test
    fun GIVEN_appending_sink_WHEN_opened_over_missing_file_THEN_nothing_is_downloaded() = runTest {
        fake.putDirectory("/ext/assets")

        api.sink(Path("/ext/assets/a.png"), append = true).use { sink ->
            sink.write(bufferOf(1), 1)
        }

        assertEquals(emptyList(), fake.requestsOf("read"))
        assertContentEquals(byteArrayOf(1), fake.fileContentOrNull("/ext/assets/a.png"))
    }

    /**
     * Appending rewrites the whole file, so an existing file that will not
     * download has to stop the sink from opening. Carrying on with an empty
     * start would replace its content instead of extending it.
     */
    @Test
    fun GIVEN_appending_sink_WHEN_opened_over_an_unreadable_existing_file_THEN_throws_IOException() = runTest {
        fake.putFile("/ext/assets/a.png", byteArrayOf(7, 8))
        fake.refusedRequests += "read /ext/assets/a.png"

        assertFailsWith<IOException> { api.sink(Path("/ext/assets/a.png"), append = true) }

        assertContentEquals(byteArrayOf(7, 8), fake.fileContentOrNull("/ext/assets/a.png"))
    }

    @Test
    fun GIVEN_invalid_path_WHEN_sink_opened_THEN_throws_IOException_without_touching_the_bar() = runTest {
        assertFailsWith<IOException> { api.sink(Path("/ext/../etc/passwd")) }

        assertEquals(emptyList(), fake.requests)
    }
}
