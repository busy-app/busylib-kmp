package net.flipper.bridge.connection.screens.dashboard.drawtool

import kotlinx.io.files.Path
import net.flipper.core.busylib.ktx.common.writeFileBytes

/**
 * Fills a collection with files shaped exactly as
 * [net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout] describes them:
 * flat PNGs directly in the collection directory, a UTC-named status file next
 * to the shared list preview.
 *
 * Write order is status file first, preview second. The status file is what a
 * reader reports, so an interrupted generation leaves a listable status behind
 * rather than a preview of a status that is not there.
 *
 * The status name comes from the layout, not from here: it is the current UTC
 * time at second resolution, so two generations within the same second address
 * the same file.
 */
class DrawToolSampleStatusWriter {
    /** Returns the path of the written status file. */
    suspend fun write(
        source: DrawToolCollectionSource,
        content: ByteArray
    ): Path {
        source.fileSystem.createDirectories(source.collectionPath)
        val statusFilePath = source.layout.getStatusFilePath()
        source.fileSystem.writeFileBytes(
            path = statusFilePath,
            content = content
        )
        source.fileSystem.writeFileBytes(
            path = source.layout.getPreviewFilePath(),
            content = content
        )
        return statusFilePath
    }
}
