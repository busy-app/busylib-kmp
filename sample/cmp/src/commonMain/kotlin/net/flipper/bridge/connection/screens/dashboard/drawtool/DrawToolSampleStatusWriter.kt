package net.flipper.bridge.connection.screens.dashboard.drawtool

import kotlinx.serialization.json.Json
import net.flipper.core.busylib.ktx.common.encodeToFile
import net.flipper.core.busylib.ktx.common.listOrEmpty
import net.flipper.core.busylib.ktx.common.writeFileBytes
import net.flipper.tools.drawtool.collection.model.DrawToolProjectFile
import net.flipper.tools.drawtool.status.util.DrawToolStatusIdGenerator
import kotlin.time.Instant

/**
 * Fills a collection with a status directory shaped exactly as
 * [net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout] describes it.
 *
 * Write order follows the spec: content first, `project.json` last. That file
 * is the commit point of a status — a directory without a parseable one does
 * not exist for readers — so an interrupted generation leaves no half-status
 * behind for the reader buttons to trip over.
 */
class DrawToolSampleStatusWriter(
    private val json: Json,
    private val statusIdGenerator: DrawToolStatusIdGenerator
) {
    /**
     * A status id free within [source]. Existing directory names are taken as
     * they are: a name that is not a valid id cannot collide with a generated
     * one anyway, and filtering them out would only hide dirt from the log.
     */
    private suspend fun generateStatusId(source: DrawToolCollectionSource): String {
        val existingNames = source.fileSystem
            .listOrEmpty(source.collectionPath)
            .map { childPath -> childPath.name }
        return statusIdGenerator.generateFree(existingNames).getOrThrow()
    }

    /** Returns the id of the generated status. */
    suspend fun write(
        source: DrawToolCollectionSource,
        frameCount: Int,
        frameContent: ByteArray,
        updatedAt: Instant
    ): String {
        val statusId = generateStatusId(source)
        source.fileSystem.createDirectories(source.layout.getStatusDirectoryPath(statusId))
        repeat(frameCount) { frameIndex ->
            source.fileSystem.writeFileBytes(
                path = source.layout.getFrameFilePath(statusId, frameIndex),
                content = frameContent
            )
        }
        source.fileSystem.writeFileBytes(
            path = source.layout.getPreviewFilePath(statusId),
            content = frameContent
        )
        json.encodeToFile(
            fileSystem = source.fileSystem,
            path = source.layout.getProjectFilePath(statusId),
            value = DrawToolProjectFile.of(updatedAt)
        ).getOrThrow()
        return statusId
    }
}
