package net.flipper.tools.drawtool.collection.api

import kotlinx.io.files.Path

/**
 * Path layout of a single status inside a collection directory. Temporary
 * and trash directories use a dotted prefix so they are never mistaken for
 * a status directory (status directories are 16 hex characters).
 */
class DrawToolStatusDirectoryLayout(
    private val collectionPath: Path
) {
    fun getStatusDirectoryPath(statusId: String): Path {
        return Path(collectionPath, statusId)
    }

    fun getTemporaryDirectoryPath(statusId: String): Path {
        return Path(collectionPath, ".tmp.$statusId")
    }

    fun getTrashDirectoryPath(statusId: String): Path {
        return Path(collectionPath, ".trash.$statusId")
    }

    fun getAnimationFilePath(statusId: String): Path {
        return Path(collectionPath, statusId, ANIMATION_FILE_NAME)
    }

    fun getAssetsDirectoryPath(statusId: String): Path {
        return Path(collectionPath, statusId, ASSETS_DIRECTORY_NAME)
    }

    fun getFrameFilePath(statusId: String, index: Int): Path {
        return Path(collectionPath, statusId, frameFileName(index))
    }

    fun getPreviewFilePath(statusId: String): Path {
        return Path(collectionPath, statusId, "preview", "current.png")
    }

    companion object {
        internal const val ANIMATION_FILE_NAME = "status.anim"
        internal const val ASSETS_DIRECTORY_NAME = "assets"
        internal const val FRAME_INDEX_LENGTH = 3
        internal val FRAME_FILE_REGEX = Regex("""frame\d{3}\.png""")
        internal fun frameFileName(index: Int): String {
            return "frame${index.toString().padStart(FRAME_INDEX_LENGTH, '0')}.png"
        }
    }
}
