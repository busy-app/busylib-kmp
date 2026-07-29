package net.flipper.tools.drawtool.layout.api

import kotlinx.io.files.Path
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout

/**
 * The layout used for a client-side collection (mobile/macOS/desktop). To
 * address the collection on the bar itself, pass
 * [DrawToolStatusDirectoryLayout.BUBSYBAR_DRAWTOOL_PATH] as [collectionPath].
 */
class DefaultDrawToolStatusDirectoryLayout(
    private val collectionPath: Path
) : DrawToolStatusDirectoryLayout {
    override fun getStatusDirectoryPath(statusId: String): Path {
        return Path(
            collectionPath,
            statusId
        )
    }

    override fun getTemporaryDirectoryPath(statusId: String): Path {
        return Path(
            collectionPath,
            ".tmp.$statusId"
        )
    }

    override fun getTrashDirectoryPath(statusId: String): Path {
        return Path(
            collectionPath,
            ".trash.$statusId"
        )
    }

    override fun getAnimationFilePath(statusId: String): Path {
        return Path(
            collectionPath,
            statusId,
            DrawToolStatusDirectoryLayout.ANIMATION_FILE_NAME
        )
    }

    override fun getAssetsDirectoryPath(statusId: String): Path {
        return Path(
            collectionPath,
            statusId,
            DrawToolStatusDirectoryLayout.ASSETS_DIRECTORY_NAME
        )
    }

    override fun getFrameFilePath(statusId: String, index: Int): Path {
        return Path(
            collectionPath,
            statusId,
            DrawToolStatusDirectoryLayout.frameFileName(index)
        )
    }

    override fun getPreviewFilePath(statusId: String): Path {
        return Path(
            collectionPath,
            statusId,
            DrawToolStatusDirectoryLayout.PREVIEW_FILE_NAME
        )
    }

    override fun getProjectFilePath(statusId: String): Path {
        return Path(
            collectionPath,
            statusId,
            DrawToolStatusDirectoryLayout.PROJECT_FILE_NAME
        )
    }
}
