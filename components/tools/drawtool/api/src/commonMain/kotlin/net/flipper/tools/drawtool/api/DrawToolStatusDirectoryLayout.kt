package net.flipper.tools.drawtool.api

import kotlinx.io.files.Path
import net.flipper.tools.drawtool.api.model.DrawToolStoredFile

/**
 * Paths of a single status inside a collection directory. Temporary and trash
 * directories carry a dotted prefix, so they can never be read as a status
 * directory — those are named by a 16 hex character status id.
 */
interface DrawToolStatusDirectoryLayout {

    fun getPreviewFilePath(): Path
    fun getStatusFilePath(): Path
    fun getStoredFilePath(file: DrawToolStoredFile): Path

    companion object {

        /** List preview at the root of a status directory. */
        const val PREVIEW_FILE_NAME = "temp.png"

        /** Extension of every file the Draw tool stores. */
        const val PNG_EXTENSION = ".png"

        /** Matches names produced by [getStatusFilePath]. */
        val STATUS_FILE_REGEX = Regex("""^\d{4}-\d{2}-\d{2}_\d{2}_\d{2}_\d{2}\.png$""")

        /**
         * The Draw tool collection on the bar itself. Unlike a client
         * collection it is not keyed by serial — a bar holds only its own
         * statuses.
         */
        val BUSYBAR_DRAWTOOL_PATH = Path("/ext", "user_assets", "draw_tool")
    }
}
