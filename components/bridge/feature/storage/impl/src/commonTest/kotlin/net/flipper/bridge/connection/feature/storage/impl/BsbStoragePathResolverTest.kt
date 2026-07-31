package net.flipper.bridge.connection.feature.storage.impl

import kotlinx.io.IOException
import kotlinx.io.files.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BsbStoragePathResolverTest {
    private val resolver = BsbStoragePathResolver()

    @Test
    fun GIVEN_mount_point_WHEN_resolved_THEN_returns_mount_point() {
        assertEquals("/ext", resolver.resolveOrNull(Path("/ext")))
    }

    @Test
    fun GIVEN_nested_path_WHEN_resolved_THEN_returns_device_path() {
        val devicePath = resolver.resolveOrNull(Path("/ext/user_assets/busy_draw/frame001.png"))

        assertEquals("/ext/user_assets/busy_draw/frame001.png", devicePath)
    }

    /**
     * Building a path from segments uses the separator of the host platform,
     * while the bar only ever understands `/`. The join must also not double
     * the separator between the mount point and the first name.
     */
    @Test
    fun GIVEN_path_built_from_segments_WHEN_resolved_THEN_names_are_joined_with_single_forward_slash() {
        assertEquals("/ext/a/b", resolver.resolveOrNull(Path("/ext", "a", "b")))
    }

    /**
     * There is no working directory on the bar to resolve a relative path
     * against. It is worth pinning on its own because the platforms disagree
     * on the shape of such a path: on JVM the topmost name simply has no
     * parent, while native ends the chain at `.`.
     */
    @Test
    fun GIVEN_relative_path_WHEN_resolved_THEN_returns_null() {
        assertNull(resolver.resolveOrNull(Path("ext/a")))
    }

    @Test
    fun GIVEN_relative_single_name_WHEN_resolved_THEN_returns_null() {
        assertNull(resolver.resolveOrNull(Path("ext")))
    }

    /**
     * Discarding the topmost element of the parent chain is not enough by
     * itself: a relative path whose second name happens to be the mount point
     * name would then read exactly like a rooted one and escape validation.
     */
    @Test
    fun GIVEN_relative_path_repeating_the_mount_point_name_WHEN_resolved_THEN_returns_null() {
        assertNull(resolver.resolveOrNull(Path("ext/ext/assets")))
    }

    @Test
    fun GIVEN_path_outside_mount_point_WHEN_resolved_THEN_returns_null() {
        assertNull(resolver.resolveOrNull(Path("/etc/passwd")))
    }

    /** `/extra` merely starts like the mount point and must not pass as one. */
    @Test
    fun GIVEN_directory_sharing_the_mount_point_prefix_WHEN_resolved_THEN_returns_null() {
        assertNull(resolver.resolveOrNull(Path("/extra/a")))
    }

    @Test
    fun GIVEN_filesystem_root_WHEN_resolved_THEN_returns_null() {
        assertNull(resolver.resolveOrNull(Path("/")))
    }

    /**
     * The bar follows a path as given, so a parent traversal would walk out of
     * the mount point instead of being normalized away.
     */
    @Test
    fun GIVEN_parent_traversal_segment_WHEN_resolved_THEN_returns_null() {
        assertNull(resolver.resolveOrNull(Path("/ext/../etc/passwd")))
    }

    @Test
    fun GIVEN_trailing_parent_traversal_segment_WHEN_resolved_THEN_returns_null() {
        assertNull(resolver.resolveOrNull(Path("/ext/assets/..")))
    }

    @Test
    fun GIVEN_current_directory_segment_WHEN_resolved_THEN_returns_null() {
        assertNull(resolver.resolveOrNull(Path("/ext/./assets")))
    }

    /** Only `.` and `..` are traversal; a name of more dots is a plain name. */
    @Test
    fun GIVEN_name_of_three_dots_WHEN_resolved_THEN_is_accepted() {
        assertEquals("/ext/...", resolver.resolveOrNull(Path("/ext/...")))
    }

    @Test
    fun GIVEN_names_of_supported_characters_WHEN_resolved_THEN_are_accepted() {
        val supportedNames = listOf("Frame001.PNG", "busy-draw", "busy_draw", "project.json", "9")
        supportedNames.forEach { name ->
            assertEquals("/ext/$name", resolver.resolveOrNull(Path("/ext/$name")), name)
        }
    }

    @Test
    fun GIVEN_names_of_unsupported_characters_WHEN_resolved_THEN_return_null() {
        val unsupportedNames = listOf("with space", "with:colon", "with*star", "имя", "with?mark")
        unsupportedNames.forEach { name ->
            assertNull(resolver.resolveOrNull(Path("/ext/$name")), name)
        }
    }

    @Test
    fun GIVEN_invalid_path_WHEN_resolve_THEN_throws_IOException() {
        assertFailsWith<IOException> { resolver.resolve(Path("/etc/passwd")) }
    }

    @Test
    fun GIVEN_valid_path_WHEN_resolve_THEN_returns_device_path() {
        assertEquals("/ext/assets/a.png", resolver.resolve(Path("/ext/assets/a.png")))
    }

    /** The mount point is not created, so it is not a level to walk. */
    @Test
    fun GIVEN_mount_point_WHEN_levels_resolved_THEN_returns_no_levels() {
        assertEquals(emptyList(), resolver.resolveLevels(Path("/ext")))
    }

    /**
     * `mkdir` on the bar creates one level at a time, so the order is part of
     * the contract: a deeper level cannot be created before its parent.
     */
    @Test
    fun GIVEN_nested_path_WHEN_levels_resolved_THEN_returns_levels_outermost_first() {
        assertEquals(
            listOf("/ext/user_assets", "/ext/user_assets/busy_draw", "/ext/user_assets/busy_draw/preview"),
            resolver.resolveLevels(Path("/ext/user_assets/busy_draw/preview"))
        )
    }

    @Test
    fun GIVEN_single_level_path_WHEN_levels_resolved_THEN_returns_that_level() {
        assertEquals(listOf("/ext/assets"), resolver.resolveLevels(Path("/ext/assets")))
    }

    @Test
    fun GIVEN_invalid_path_WHEN_levels_resolved_THEN_throws_IOException() {
        assertFailsWith<IOException> { resolver.resolveLevels(Path("/ext/../etc")) }
    }

    /**
     * Metadata of the mount point cannot come from listing its parent, so
     * recognising it must not be a prefix match over everything under it.
     */
    @Test
    fun GIVEN_device_paths_WHEN_checked_for_mount_point_THEN_only_the_mount_point_matches() {
        assertTrue(resolver.isRoot("/ext"))
        assertFalse(resolver.isRoot("/ext/assets"))
        assertFalse(resolver.isRoot("/extra"))
        assertFalse(resolver.isRoot("/"))
    }
}
