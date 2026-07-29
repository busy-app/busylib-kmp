package net.flipper.tools.drawtool.collection.util

import kotlinx.io.files.Path
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.api.model.DrawToolFileType
import net.flipper.tools.drawtool.layout.api.DefaultDrawToolStatusDirectoryLayout
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The resolver classifies the absolute paths the status reader walks, so every
 * case is built through the layout that produced them on write.
 */
class DrawToolFileTypeResolverTest {
    private val statusId = "abcdef0123456789"
    private val layout: DrawToolStatusDirectoryLayout = DefaultDrawToolStatusDirectoryLayout(
        collectionPath = Path("/ext", "user_assets", "busy_draw")
    )
    private val resolver = DrawToolFileTypeResolver()

    /**
     * Whatever the layout writes as a frame has to read back as a frame: an
     * animated status is replayed frame by frame, so the writer and the reader
     * must agree on every index the layout can name.
     */
    @Test
    fun GIVEN_frame_files_written_by_the_layout_WHEN_resolved_THEN_all_return_frame() {
        val frameIndices = listOf(0, 1, 9, 10, 99, 100, 137, 999)

        frameIndices.forEach { index ->
            val framePath = layout.getFrameFilePath(statusId, index)

            assertEquals(DrawToolFileType.FRAME, resolver.resolve(framePath), framePath.toString())
        }
    }

    @Test
    fun GIVEN_animation_file_WHEN_resolved_THEN_returns_animation() {
        val animationPath = layout.getAnimationFilePath(statusId)

        assertEquals(DrawToolFileType.ANIMATION, resolver.resolve(animationPath))
    }

    @Test
    fun GIVEN_preview_file_WHEN_resolved_THEN_returns_preview() {
        val previewPath = layout.getPreviewFilePath(statusId)

        assertEquals(DrawToolFileType.PREVIEW, resolver.resolve(previewPath))
    }

    /** An asset is recognised by its directory: it is named by a content hash. */
    @Test
    fun GIVEN_file_in_assets_directory_WHEN_resolved_THEN_returns_asset() {
        val assetPath = Path(layout.getAssetsDirectoryPath(statusId), "9f2c4a1b8e7d.png")

        assertEquals(DrawToolFileType.ASSET, resolver.resolve(assetPath))
    }

    /**
     * The project file carries the metadata of the status itself and is read
     * separately, so it must not be reported as a file of the status.
     */
    @Test
    fun GIVEN_project_file_WHEN_resolved_THEN_returns_other() {
        val projectPath = layout.getProjectFilePath(statusId)

        assertEquals(DrawToolFileType.OTHER, resolver.resolve(projectPath))
    }

    /**
     * Files written by a newer client survive a read/save round trip, which
     * requires them to be classified as unrecognised rather than misread.
     */
    @Test
    fun GIVEN_unknown_file_in_status_directory_WHEN_resolved_THEN_returns_other() {
        val unknownPath = Path(layout.getStatusDirectoryPath(statusId), "effects.json")

        assertEquals(DrawToolFileType.OTHER, resolver.resolve(unknownPath))
    }

    /**
     * The bar plays frames by their padded index, so a differently padded name
     * is not a frame this client wrote and must not be replayed as one.
     */
    @Test
    fun GIVEN_frame_file_with_a_differently_padded_index_WHEN_resolved_THEN_returns_other() {
        val statusDirectory = layout.getStatusDirectoryPath(statusId)

        assertEquals(DrawToolFileType.OTHER, resolver.resolve(Path(statusDirectory, "frame1.png")))
        assertEquals(DrawToolFileType.OTHER, resolver.resolve(Path(statusDirectory, "frame0001.png")))
    }

    /**
     * The frame index is written in ASCII, and the digit class of the regex is
     * not the same on every target once other scripts are involved.
     */
    @Test
    fun GIVEN_frame_file_with_non_ascii_digits_WHEN_resolved_THEN_returns_other() {
        val lookalikePath = Path(layout.getStatusDirectoryPath(statusId), "frame٠٠١.png")

        assertEquals(DrawToolFileType.OTHER, resolver.resolve(lookalikePath))
    }

    /** Assets are flat, so a nested file is not one this client wrote. */
    @Test
    fun GIVEN_file_in_a_subdirectory_of_assets_WHEN_resolved_THEN_returns_other() {
        val nestedPath = Path(layout.getAssetsDirectoryPath(statusId), "nested", "9f2c4a1b.png")

        assertEquals(DrawToolFileType.OTHER, resolver.resolve(nestedPath))
    }

    /**
     * A name that has no directory above it is the case where asking for the
     * parent yields nothing, which must not be mistaken for a directory name.
     */
    @Test
    fun GIVEN_file_name_without_a_parent_directory_WHEN_resolved_THEN_is_classified_by_its_name() {
        assertEquals(DrawToolFileType.FRAME, resolver.resolve(Path("frame001.png")))
        assertEquals(DrawToolFileType.PREVIEW, resolver.resolve(Path("preview.png")))
        assertEquals(DrawToolFileType.OTHER, resolver.resolve(Path("notes.txt")))
    }

    /** A path that names no file at all is not a file of a status. */
    @Test
    fun GIVEN_paths_naming_no_file_WHEN_resolved_THEN_return_other() {
        assertEquals(DrawToolFileType.OTHER, resolver.resolve(Path("/")))
        assertEquals(DrawToolFileType.OTHER, resolver.resolve(Path("")))
    }

    /** Only the exact packed animation name is the animation of the status. */
    @Test
    fun GIVEN_file_name_ending_with_the_animation_name_WHEN_resolved_THEN_returns_other() {
        val lookalikePath = Path(layout.getStatusDirectoryPath(statusId), "old.status.anim")

        assertEquals(DrawToolFileType.OTHER, resolver.resolve(lookalikePath))
    }

    /** Only the exact preview name is the preview of the status. */
    @Test
    fun GIVEN_file_name_ending_with_the_preview_name_WHEN_resolved_THEN_returns_other() {
        val lookalikePath = Path(layout.getStatusDirectoryPath(statusId), "old.preview.png")

        assertEquals(DrawToolFileType.OTHER, resolver.resolve(lookalikePath))
    }

    /** Assets live in `assets`; a directory merely ending in it is not that one. */
    @Test
    fun GIVEN_file_in_directory_ending_with_the_assets_name_WHEN_resolved_THEN_returns_other() {
        val lookalikePath = Path(layout.getStatusDirectoryPath(statusId), "oldassets", "9f2c4a1b.png")

        assertEquals(DrawToolFileType.OTHER, resolver.resolve(lookalikePath))
    }
}
