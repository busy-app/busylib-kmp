package net.flipper.tools.drawtool.collection.util

import dev.zacsweers.metro.Inject
import kotlinx.io.files.Path
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.api.model.DrawToolFileType

/**
 * Derives the role of a status file from its name and place in the directory —
 * the layout is the only source of that, `project.json` stores no per-file type.
 *
 * A name match wins over a location match, so a `frameNNN.png` sitting under
 * `assets/` still counts as a frame.
 */
@Inject
class DrawToolFileTypeResolver {
    private fun isAnimation(path: Path): Boolean {
        return path.name == DrawToolStatusDirectoryLayout.ANIMATION_FILE_NAME
    }

    private fun isAsset(path: Path): Boolean {
        return path.parent?.name == DrawToolStatusDirectoryLayout.ASSETS_DIRECTORY_NAME
    }

    private fun isPreview(path: Path): Boolean {
        return path.name == DrawToolStatusDirectoryLayout.PREVIEW_FILE_NAME
    }

    private fun isFrame(path: Path): Boolean {
        return DrawToolStatusDirectoryLayout
            .FRAME_FILE_REGEX
            .matches(path.name)
    }

    /** Role of [path], [DrawToolFileType.OTHER] when nothing in the layout matches. */
    fun resolve(path: Path): DrawToolFileType {
        return when {
            isFrame(path) -> DrawToolFileType.FRAME
            isAnimation(path) -> DrawToolFileType.ANIMATION
            isAsset(path) -> DrawToolFileType.ASSET
            isPreview(path) -> DrawToolFileType.PREVIEW

            else -> DrawToolFileType.OTHER
        }
    }
}
