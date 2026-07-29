package net.flipper.tools.drawtool.collection.util

import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.Json
import net.flipper.core.busylib.ktx.common.decodeFromFile
import net.flipper.core.busylib.ktx.common.walkRegularFiles
import net.flipper.core.busylib.ktx.io.FlipperFileSystem
import net.flipper.core.busylib.log.warn
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.api.model.DrawToolStatus
import net.flipper.tools.drawtool.api.model.DrawToolStoredFile
import net.flipper.tools.drawtool.collection.model.DrawToolProjectFile

/**
 * Reads one status directory into a [DrawToolStatus].
 *
 * `project.json` decides existence: without a parseable one the status is
 * reported absent even if content files are already on disk, which is what makes
 * an interrupted save invisible to readers. Files come back in a stable path
 * order, so two reads of an unchanged directory compare equal.
 */
class DrawToolStatusReader(
    private val fileSystem: FlipperFileSystem,
    private val json: Json,
    private val fileTypeResolver: DrawToolFileTypeResolver,
    private val layout: DrawToolStatusDirectoryLayout
) {
    private suspend fun readProjectFileOrNull(statusId: String): DrawToolProjectFile? {
        val projectPath = layout.getProjectFilePath(statusId)
        if (!fileSystem.exists(projectPath)) return null
        return json
            .decodeFromFile<DrawToolProjectFile>(fileSystem, projectPath)
            .onFailure { readError -> warn { "Unreadable $projectPath, treating the status as absent: $readError" } }
            .getOrNull()
    }

    private suspend fun readStatusFiles(statusId: String): List<DrawToolStoredFile> {
        val statusDirectory = layout.getStatusDirectoryPath(statusId)
        return fileSystem.walkRegularFiles(statusDirectory)
            .map { path ->
                DrawToolStoredFile(
                    type = fileTypeResolver.resolve(path),
                    path = path
                )
            }
            .sortedBy { storedFile -> storedFile.path.toString() }
    }

    /** The status [statusId], or `null` when its `project.json` is missing or broken. */
    suspend fun readDrawToolStatusOrNull(statusId: String): DrawToolStatus? {
        val projectFile = readProjectFileOrNull(statusId) ?: return null
        val storedFiles = readStatusFiles(statusId)
        return DrawToolStatus(
            id = statusId,
            updatedAt = projectFile.updatedAt,
            files = storedFiles.toImmutableList()
        )
    }
}
