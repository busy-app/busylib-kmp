package net.flipper.tools.drawtool.status.util

import dev.zacsweers.metro.Inject
import kotlinx.io.files.Path
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.error
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.api.model.DrawToolFileType

@Inject
class DrawToolFileTypeResolver : LogTagProvider by TaggedLogger("DrawToolFileTypeResolver") {
    private fun isPreview(path: Path): Boolean {
        return path.name == DrawToolStatusDirectoryLayout.PREVIEW_FILE_NAME
    }

    private fun isStatus(path: Path): Boolean {
        return DrawToolStatusDirectoryLayout
            .STATUS_FILE_REGEX
            .matches(path.name)
    }

    fun resolve(path: Path): DrawToolFileType? {
        return when {
            isStatus(path) -> DrawToolFileType.STATUS
            isPreview(path) -> DrawToolFileType.PREVIEW
            else -> {
                error { "#resolve could not resolve DrawToolFileType $path" }
                null
            }
        }
    }
}
