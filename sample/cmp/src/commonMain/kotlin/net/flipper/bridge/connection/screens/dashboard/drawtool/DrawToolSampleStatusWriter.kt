package net.flipper.bridge.connection.screens.dashboard.drawtool

import kotlinx.io.files.Path
import net.flipper.core.busylib.ktx.common.writeFileBytes

/**
 * Writes a status and the shared preview as flat PNGs in the collection
 * directory, status first: an interrupted run then leaves a listable status
 * rather than a preview of nothing.
 *
 * The name comes from the layout — UTC at second resolution, so two runs within
 * one second hit the same file.
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
