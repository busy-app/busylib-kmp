package net.flipper.bridge.connection.screens.dashboard.drawtool

import kotlinx.io.files.Path
import net.flipper.tools.drawtool.api.model.DrawToolDirectoryContents

/**
 * Renders a collection listing as console text. A file that was written but is
 * missing from the output means its name matched neither the preview nor the
 * status naming of the layout, which is the failure worth seeing in the log.
 */
internal fun formatDrawToolStatuses(
    target: DrawToolStorageTarget,
    collectionPath: Path,
    contents: DrawToolDirectoryContents
): String {
    return buildString {
        appendLine("${target.title} collection $collectionPath: ${contents.files.size} file(s)")
        contents.files.forEach { storedFile ->
            appendLine("- ${storedFile.type} ${storedFile.path.name}")
        }
    }.trimEnd()
}
