package net.flipper.bridge.connection.feature.storage.impl

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.io.IOException
import kotlinx.io.files.FileNotFoundException
import kotlinx.io.files.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FStorageFeatureApiImplMutationTest {
    private val fake = FakeFRpcStorageApi()

    private val api = FStorageFeatureApiImpl(
        storageApi = fake,
        pathResolver = BsbStoragePathResolver()
    )

    /**
     * `mkdir` on the bar creates a single level, so a tree is walked from the
     * mount point down — a deeper level cannot be created before its parent.
     */
    @Test
    fun GIVEN_missing_tree_WHEN_created_THEN_levels_are_created_outermost_first() = runTest {
        api.createDirectories(Path("/ext/user_assets/busy_draw/preview"))

        assertEquals(
            listOf(
                "mkdir /ext/user_assets",
                "mkdir /ext/user_assets/busy_draw",
                "mkdir /ext/user_assets/busy_draw/preview"
            ),
            fake.requestsOf("mkdir")
        )
        assertTrue(fake.hasDirectory("/ext/user_assets/busy_draw/preview"))
    }

    @Test
    fun GIVEN_existing_directory_WHEN_created_THEN_bar_is_not_asked_to_create_anything() = runTest {
        fake.putDirectory("/ext/assets")

        api.createDirectories(Path("/ext/assets"))

        assertEquals(emptyList(), fake.requestsOf("mkdir"))
    }

    @Test
    fun GIVEN_mount_point_WHEN_created_THEN_bar_is_not_asked_to_create_anything() = runTest {
        api.createDirectories(Path("/ext"))

        assertEquals(emptyList(), fake.requestsOf("mkdir"))
    }

    @Test
    fun GIVEN_existing_directory_WHEN_created_with_mustCreate_THEN_throws_IOException() = runTest {
        fake.putDirectory("/ext/assets")

        assertFailsWith<IOException> {
            api.createDirectories(Path("/ext/assets"), mustCreate = true)
        }
    }

    /** A file where a directory is wanted cannot be resolved by creating more. */
    @Test
    fun GIVEN_existing_file_at_the_path_WHEN_created_THEN_throws_IOException() = runTest {
        fake.putFile("/ext/assets", byteArrayOf(1))

        assertFailsWith<IOException> { api.createDirectories(Path("/ext/assets")) }
    }

    /**
     * The bar refuses a level that already exists, and on a partially existing
     * tree that refusal is the normal case rather than a failure.
     */
    @Test
    fun GIVEN_partially_existing_tree_WHEN_created_THEN_refused_levels_are_ignored() = runTest {
        fake.putDirectory("/ext/user_assets")

        api.createDirectories(Path("/ext/user_assets/busy_draw"))

        assertTrue(fake.hasDirectory("/ext/user_assets/busy_draw"))
    }

    @Test
    fun GIVEN_bar_refusing_every_level_WHEN_created_THEN_throws_IOException() = runTest {
        fake.refusedRequests += "mkdir /ext/user_assets"
        fake.refusedRequests += "mkdir /ext/user_assets/busy_draw"

        assertFailsWith<IOException> {
            api.createDirectories(Path("/ext/user_assets/busy_draw"))
        }
    }

    @Test
    fun GIVEN_invalid_path_WHEN_created_THEN_throws_IOException_without_touching_the_bar() = runTest {
        assertFailsWith<IOException> { api.createDirectories(Path("/etc/init.d")) }

        assertEquals(emptyList(), fake.requests)
    }

    /**
     * Two sync tasks may well be preparing the same tree at once. Neither may
     * fail because the other got there first — that is exactly why a refused
     * level is tolerated and only the end state is checked.
     */
    @Test
    fun GIVEN_two_callers_creating_the_same_tree_WHEN_run_concurrently_THEN_both_succeed() = runTest {
        fake.onRequest = { yield() }
        val directoryPath = Path("/ext/user_assets/busy_draw")

        coroutineScope {
            listOf(
                async { api.createDirectories(directoryPath) },
                async { api.createDirectories(directoryPath) }
            ).awaitAll()
        }

        assertTrue(fake.hasDirectory("/ext/user_assets/busy_draw"))
    }

    @Test
    fun GIVEN_existing_file_WHEN_deleted_THEN_file_is_gone() = runTest {
        fake.putFile("/ext/assets/a.png", byteArrayOf(1))

        api.delete(Path("/ext/assets/a.png"))

        assertFalse(fake.hasFile("/ext/assets/a.png"))
    }

    @Test
    fun GIVEN_missing_path_WHEN_deleted_requiring_existence_THEN_throws_FileNotFoundException() = runTest {
        fake.putDirectory("/ext/assets")

        assertFailsWith<FileNotFoundException> {
            api.delete(Path("/ext/assets/a.png"), mustExist = true)
        }
    }

    @Test
    fun GIVEN_missing_path_WHEN_deleted_without_requiring_existence_THEN_does_not_fail() = runTest {
        fake.putDirectory("/ext/assets")

        api.delete(Path("/ext/assets/a.png"), mustExist = false)
    }

    /**
     * A refusal on a path that is still there is a real failure, and reporting
     * it as an absence would let a caller treat the deletion as done.
     */
    @Test
    fun GIVEN_undeletable_existing_file_WHEN_deleted_THEN_throws_an_IOException_that_is_not_FileNotFound() = runTest {
        fake.putFile("/ext/assets/a.png", byteArrayOf(1))
        fake.refusedRequests += "remove /ext/assets/a.png"

        val error = assertFailsWith<IOException> { api.delete(Path("/ext/assets/a.png")) }

        assertFalse(error is FileNotFoundException)
        assertTrue(fake.hasFile("/ext/assets/a.png"))
    }

    /** Deletion is not recursive, so a non-empty directory survives it. */
    @Test
    fun GIVEN_non_empty_directory_WHEN_deleted_THEN_throws_IOException_and_directory_survives() = runTest {
        fake.putFile("/ext/assets/a.png", byteArrayOf(1))

        assertFailsWith<IOException> { api.delete(Path("/ext/assets")) }

        assertTrue(fake.hasDirectory("/ext/assets"))
    }

    @Test
    fun GIVEN_invalid_path_WHEN_deleted_THEN_throws_IOException_without_touching_the_bar() = runTest {
        assertFailsWith<IOException> { api.delete(Path("/ext/./assets")) }

        assertEquals(emptyList(), fake.requests)
    }

    @Test
    fun GIVEN_existing_file_WHEN_moved_THEN_content_is_at_the_destination() = runTest {
        fake.putFile("/ext/assets/a.png", byteArrayOf(1, 2))

        api.atomicMove(Path("/ext/assets/a.png"), Path("/ext/assets/b.png"))

        assertFalse(fake.hasFile("/ext/assets/a.png"))
        assertContentEquals(byteArrayOf(1, 2), fake.fileContentOrNull("/ext/assets/b.png"))
    }

    @Test
    fun GIVEN_missing_source_WHEN_moved_THEN_throws_FileNotFoundException() = runTest {
        fake.putDirectory("/ext/assets")

        assertFailsWith<FileNotFoundException> {
            api.atomicMove(Path("/ext/assets/a.png"), Path("/ext/assets/b.png"))
        }
    }

    @Test
    fun GIVEN_refused_move_of_existing_source_WHEN_moved_THEN_throws_IOException_not_FileNotFound() = runTest {
        fake.putFile("/ext/assets/a.png", byteArrayOf(1))
        fake.refusedRequests += "rename /ext/assets/a.png"

        val error = assertFailsWith<IOException> {
            api.atomicMove(Path("/ext/assets/a.png"), Path("/ext/assets/b.png"))
        }

        assertFalse(error is FileNotFoundException)
        assertTrue(fake.hasFile("/ext/assets/a.png"))
    }

    @Test
    fun GIVEN_invalid_destination_WHEN_moved_THEN_throws_IOException_without_touching_the_bar() = runTest {
        fake.putFile("/ext/assets/a.png", byteArrayOf(1))

        assertFailsWith<IOException> {
            api.atomicMove(Path("/ext/assets/a.png"), Path("/etc/passwd"))
        }

        assertEquals(emptyList(), fake.requests)
    }
}
