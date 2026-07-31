package net.flipper.bridge.connection.screens.dashboard.drawtool

import kotlinx.io.files.Path
import net.flipper.tools.drawtool.api.model.DrawToolDirectoryContents
import net.flipper.tools.drawtool.api.model.DrawToolStoredFile

private val DrawToolStoredFile.roleLabel: String
    get() = when (this) {
        is DrawToolStoredFile.Preview -> "PREVIEW"
        is DrawToolStoredFile.Status -> "STATUS"
    }

/**
 * Renders a collection listing as console text. A written file missing here
 * matched neither naming rule of the layout — the failure worth seeing.
 */
internal fun formatDrawToolStatuses(
    target: DrawToolStorageTarget,
    collectionPath: Path,
    contents: DrawToolDirectoryContents
): String {
    return buildString {
        appendLine("${target.title} collection $collectionPath: ${contents.files.size} file(s)")
        contents.files.forEach { storedFile ->
            appendLine("- ${storedFile.roleLabel} ${storedFile.path.name}")
        }
    }.trimEnd()
}
