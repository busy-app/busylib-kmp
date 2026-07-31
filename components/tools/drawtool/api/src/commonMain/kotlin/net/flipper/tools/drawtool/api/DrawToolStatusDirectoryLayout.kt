package net.flipper.tools.drawtool.api

import kotlinx.io.files.Path
import net.flipper.tools.drawtool.api.model.DrawToolStoredFile

/**
 * Paths inside one Draw tool collection — a flat directory of PNG files, laid
 * out identically on the client and on the bar.
 *
 * Besides statuses a collection holds only the [PREVIEW_FILE_NAME] working
 * file and, mid-transfer, the dotted [TEMPORARY_FILE_NAME]; neither matches
 * [STATUS_FILE_REGEX], so nothing but a status can ever be read as one.
 */
interface DrawToolStatusDirectoryLayout {

    fun getCollectionPath(): Path

    fun getPreviewFilePath(): Path

    /** A path for a status saved now, named by the current UTC time. */
    fun getStatusFilePath(): Path

    /** The path of the status [name] refers to, whether or not it exists yet. */
    fun getStatusFilePath(name: String): Path

    /**
     * The counterpart of [file] in this collection, wherever [file] itself
     * lives — how a client status is addressed on the bar and back.
     */
    fun getStoredFilePath(file: DrawToolStoredFile): Path

    /**
     * The one in-flight transfer file of the collection: content lands here
     * first and is renamed into its status name only whole, so a listed status
     * is always a complete file.
     */
    fun getTemporaryFilePath(): Path

    companion object {

        /**
         * The current drawing, overwritten by every preview. Not a status:
         * it is never synchronized and can be discarded at any time.
         */
        const val PREVIEW_FILE_NAME = "temp.png"

        /** Extension of every file the Draw tool stores. */
        const val PNG_EXTENSION = ".png"

        /** Never matches [STATUS_FILE_REGEX]. */
        const val TEMPORARY_FILE_NAME = ".sync.tmp"

        /**
         * `YYYY-mm-dd_HH_mm_ss.png`, zero padded, always UTC — every component
         * is fixed width, so names sort chronologically as plain text.
         * Anything a listing finds that does not match is ignored, whatever
         * put it there.
         */
        val STATUS_FILE_REGEX = Regex("""^\d{4}-\d{2}-\d{2}_\d{2}_\d{2}_\d{2}\.png$""")

        /**
         * The Draw tool collection on the bar itself. Unlike a client
         * collection it is not keyed by serial — a bar holds only its own
         * statuses.
         */
        val BUSYBAR_DRAWTOOL_PATH = Path("/ext", "user_assets", "draw_tool")
    }
}
