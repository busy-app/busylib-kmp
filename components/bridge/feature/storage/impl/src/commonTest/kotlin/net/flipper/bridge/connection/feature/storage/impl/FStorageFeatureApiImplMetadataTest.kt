package net.flipper.bridge.connection.feature.storage.impl

import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.io.files.FileNotFoundException
import kotlinx.io.files.Path
import net.flipper.bridge.connection.feature.rpc.api.model.StorageListResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FStorageFeatureApiImplMetadataTest {
    private val fake = FakeFRpcStorageApi()

    private val api = FStorageFeatureApiImpl(
        storageApi = fake,
        pathResolver = BsbStoragePathResolver()
    )

    /**
     * The mount point is in no listing but its own, so its metadata cannot be
     * found the way every other path's is.
     */
    @Test
    fun GIVEN_mount_point_WHEN_metadata_requested_THEN_reports_a_directory() = runTest {
        val metadata = api.metadataOrNull(Path("/ext"))

        assertTrue(metadata != null)
        assertTrue(metadata.isDirectory)
        assertFalse(metadata.isRegularFile)
    }

    @Test
    fun GIVEN_unreachable_bar_WHEN_mount_point_metadata_requested_THEN_returns_null() = runTest {
        fake.refusedRequests += "list /ext"

        assertNull(api.metadataOrNull(Path("/ext")))
    }

    @Test
    fun GIVEN_file_on_bar_WHEN_metadata_requested_THEN_reports_a_regular_file_with_its_size() = runTest {
        fake.putFile("/ext/assets/a.png", byteArrayOf(1, 2, 3))

        val metadata = api.metadataOrNull(Path("/ext/assets/a.png"))

        assertTrue(metadata != null)
        assertTrue(metadata.isRegularFile)
        assertFalse(metadata.isDirectory)
        assertEquals(3L, metadata.size)
    }

    /** `FileMetadata` reports a size for regular files only. */
    @Test
    fun GIVEN_directory_on_bar_WHEN_metadata_requested_THEN_reports_a_directory_without_a_size() = runTest {
        fake.putDirectory("/ext/assets")

        val metadata = api.metadataOrNull(Path("/ext/assets"))

        assertTrue(metadata != null)
        assertTrue(metadata.isDirectory)
        assertEquals(-1L, metadata.size)
    }

    @Test
    fun GIVEN_missing_name_in_an_existing_directory_WHEN_metadata_requested_THEN_returns_null() = runTest {
        fake.putDirectory("/ext/assets")

        assertNull(api.metadataOrNull(Path("/ext/assets/a.png")))
    }

    @Test
    fun GIVEN_missing_parent_directory_WHEN_metadata_requested_THEN_returns_null() = runTest {
        assertNull(api.metadataOrNull(Path("/ext/nowhere/a.png")))
    }

    /**
     * A path the bar could not hold is answered locally: asking would cost a
     * round trip and could only come back refused.
     */
    @Test
    fun GIVEN_invalid_path_WHEN_metadata_requested_THEN_returns_null_without_touching_the_bar() = runTest {
        assertNull(api.metadataOrNull(Path("/etc/passwd")))
        assertEquals(emptyList(), fake.requests)
    }

    /** The bar may omit a size; that is an empty file, not a broken listing. */
    @Test
    fun GIVEN_file_listed_without_a_size_WHEN_metadata_requested_THEN_reports_zero_size() = runTest {
        fake.listingOverrides["/ext/assets"] = StorageListResponse(
            list = listOf(
                StorageListResponse.Entry(
                    type = StorageListResponse.Entry.Type.FILE,
                    name = "a.png",
                    size = null
                )
            )
        )

        val metadata = api.metadataOrNull(Path("/ext/assets/a.png"))

        assertTrue(metadata != null)
        assertTrue(metadata.isRegularFile)
        assertEquals(0L, metadata.size)
    }

    @Test
    fun GIVEN_directory_with_children_WHEN_listed_THEN_returns_their_paths() = runTest {
        fake.putFile("/ext/assets/a.png", byteArrayOf(1))
        fake.putDirectory("/ext/assets/nested")

        val children = api.list(Path("/ext/assets"))

        assertEquals(
            setOf(Path("/ext/assets/a.png"), Path("/ext/assets/nested")),
            children.toSet()
        )
    }

    @Test
    fun GIVEN_empty_directory_WHEN_listed_THEN_returns_nothing() = runTest {
        fake.putDirectory("/ext/assets")

        assertEquals(emptyList(), api.list(Path("/ext/assets")).toList())
    }

    @Test
    fun GIVEN_missing_directory_WHEN_listed_THEN_throws_FileNotFoundException() = runTest {
        assertFailsWith<FileNotFoundException> { api.list(Path("/ext/nowhere")) }
    }

    /**
     * The bar refuses a file and a missing path alike, so the two are told
     * apart afterwards — and a file must not be reported as missing.
     */
    @Test
    fun GIVEN_regular_file_WHEN_listed_THEN_throws_an_IOException_that_is_not_FileNotFound() = runTest {
        fake.putFile("/ext/assets/a.png", byteArrayOf(1))

        val error = assertFailsWith<IOException> { api.list(Path("/ext/assets/a.png")) }

        assertFalse(error is FileNotFoundException)
    }

    /** An existing directory that will not list is a failure, not an absence. */
    @Test
    fun GIVEN_unlistable_directory_WHEN_listed_THEN_throws_an_IOException_that_is_not_FileNotFound() = runTest {
        fake.putDirectory("/ext/assets")
        fake.refusedRequests += "list /ext/assets"

        val error = assertFailsWith<IOException> { api.list(Path("/ext/assets")) }

        assertFalse(error is FileNotFoundException)
    }

    @Test
    fun GIVEN_invalid_path_WHEN_listed_THEN_throws_IOException_without_touching_the_bar() = runTest {
        assertFailsWith<IOException> { api.list(Path("/ext/../etc")) }

        assertEquals(emptyList(), fake.requests)
    }

    @Test
    fun GIVEN_existing_path_WHEN_resolved_THEN_returns_the_absolute_device_path() = runTest {
        fake.putFile("/ext/assets/a.png", byteArrayOf(1))

        assertEquals(Path("/ext/assets/a.png"), api.resolve(Path("/ext", "assets", "a.png")))
    }

    @Test
    fun GIVEN_missing_path_WHEN_resolved_THEN_throws_FileNotFoundException() = runTest {
        fake.putDirectory("/ext/assets")

        assertFailsWith<FileNotFoundException> { api.resolve(Path("/ext/assets/a.png")) }
    }

    @Test
    fun GIVEN_invalid_path_WHEN_resolved_THEN_throws_IOException_without_touching_the_bar() = runTest {
        assertFailsWith<IOException> { api.resolve(Path("/etc/passwd")) }

        assertEquals(emptyList(), fake.requests)
    }
}
