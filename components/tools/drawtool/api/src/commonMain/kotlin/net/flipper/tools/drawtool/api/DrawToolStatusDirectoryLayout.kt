package net.flipper.tools.drawtool.api

import kotlinx.io.files.Path

/**
 * Paths of a single status inside a collection directory. Temporary and trash
 * directories carry a dotted prefix, so they can never be read as a status
 * directory — those are named by a 16 hex character status id.
 */
interface DrawToolStatusDirectoryLayout {
    /** `<collection>/<statusId>`, holding everything the status owns. */
    fun getStatusDirectoryPath(statusId: String): Path

    /**
     * Where a new version of the status is assembled before being swapped in,
     * so an interrupted save never leaves a half-status in its place.
     */
    fun getTemporaryDirectoryPath(statusId: String): Path

    /**
     * Where the previous version is parked during that swap, to be dropped once
     * the new one is in place.
     */
    fun getTrashDirectoryPath(statusId: String): Path

    /** The packed animation the bar plays for an animated status. */
    fun getAnimationFilePath(statusId: String): Path

    /** Scene media sources of the status, each named by its content hash. */
    fun getAssetsDirectoryPath(statusId: String): Path

    /** Render frame [index] of the status. */
    fun getFrameFilePath(statusId: String, index: Int): Path

    /** The image the status list shows. */
    fun getPreviewFilePath(statusId: String): Path

    /**
     * Metadata of the status and its commit point: written last, and a directory
     * without a parseable one does not exist for readers.
     */
    fun getProjectFilePath(statusId: String): Path

    companion object {
        /** Metadata file at the root of a status directory. */
        const val PROJECT_FILE_NAME = "project.json"

        /** Packed animation at the root of a status directory. */
        const val ANIMATION_FILE_NAME = "status.anim"

        /** Media directory inside a status directory. */
        const val ASSETS_DIRECTORY_NAME = "assets"

        /** List preview at the root of a status directory. */
        const val PREVIEW_FILE_NAME = "preview.png"

        /** Zero-padding of a frame index, so frames sort in playback order. */
        const val FRAME_INDEX_LENGTH = 3

        /** Tells a frame apart from the other files of a status. */
        val FRAME_FILE_REGEX = Regex("""frame\d{3}\.png""")

        /**
         * The Draw tool collection on the bar itself. Unlike a client
         * collection it is not keyed by serial — a bar holds only its own
         * statuses.
         */
        val BUBSYBAR_DRAWTOOL_PATH = Path("/ext", "user_assets", "busy_draw")

        /** Frame file name for [index], padded to [FRAME_INDEX_LENGTH]. */
        fun frameFileName(index: Int): String {
            return "frame${index.toString().padStart(FRAME_INDEX_LENGTH, '0')}.png"
        }
    }
}
