package net.flipper.tools.drawtool.layout.api

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.files.Path
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.api.model.DrawToolStoredFile
import kotlin.time.Clock

/**
 * The layout used for a client-side collection (mobile/macOS/desktop).
 * To address the collection on the bar itself, pass
 * [DrawToolStatusDirectoryLayout.BUSYBAR_DRAWTOOL_PATH] as [collectionPath].
 *
 * [clock] is only used to name newly saved statuses; it is a parameter so tests
 * can pin the produced name.
 */
class DefaultDrawToolStatusDirectoryLayout(
    private val collectionPath: Path,
    private val clock: Clock = Clock.System
) : DrawToolStatusDirectoryLayout {
    override fun getPreviewFilePath(): Path {
        return Path(
            collectionPath,
            DrawToolStatusDirectoryLayout.PREVIEW_FILE_NAME
        )
    }

    override fun getStatusFilePath(): Path {
        val name = clock.now()
            .toLocalDateTime(TimeZone.UTC)
            .format(STATUS_NAME_FORMAT)
        return Path(
            collectionPath,
            name + DrawToolStatusDirectoryLayout.PNG_EXTENSION
        )
    }

    override fun getStoredFilePath(file: DrawToolStoredFile): Path {
        return when (file) {
            is DrawToolStoredFile.Preview -> getPreviewFilePath()
            is DrawToolStoredFile.Status -> Path(collectionPath, file.path.name)
        }
    }

    private companion object {
        /**
         * `2026-07-29_20_51_11` — zero padded, second resolution, always UTC.
         * Every component is fixed width, so the name sorts chronologically as
         * plain text.
         */
        private val STATUS_NAME_FORMAT: DateTimeFormat<LocalDateTime>
            get() = LocalDateTime.Format {
                year()
                char('-')
                monthNumber()
                char('-')
                day()
                char('_')
                hour()
                char('_')
                minute()
                char('_')
                second()
            }
    }
}
