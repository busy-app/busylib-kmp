package net.flipper.bridge.connection.screens.dashboard.drawtool

import kotlinx.io.files.Path
import net.flipper.tools.drawtool.api.model.DrawToolDirectoryContents

private fun StringBuilder.appendStatus(status: DrawToolDirectoryContents) {
    appendLine("- ${status.id} updatedAt=${status.updatedAt} files=${status.files.size}")
    status.files.forEach { storedFile ->
        appendLine("    ${storedFile.type} ${storedFile.path}")
    }
}

/**
 * Renders a collection listing as console text. A status that was written but
 * is missing from the output means its `project.json` was not readable, which
 * is the failure worth seeing in the log.
 */
internal fun formatDrawToolStatuses(
    target: DrawToolStorageTarget,
    collectionPath: Path,
    statuses: List<DrawToolDirectoryContents>
): String {
    return buildString {
        appendLine("${target.title} collection $collectionPath: ${statuses.size} status(es)")
        statuses.forEach { status -> appendStatus(status) }
    }.trimEnd()
}
