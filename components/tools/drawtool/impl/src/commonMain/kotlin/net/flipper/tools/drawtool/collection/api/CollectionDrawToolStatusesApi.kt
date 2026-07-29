package net.flipper.tools.drawtool.collection.api

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.io.files.Path
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.core.busylib.ktx.common.deleteRecursively
import net.flipper.core.busylib.ktx.common.listOrEmpty
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.ktx.io.FlipperFileSystem
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.api.DrawToolStatusesApi
import net.flipper.tools.drawtool.api.model.DrawToolStatus
import net.flipper.tools.drawtool.collection.util.DrawToolStatusIdValidator
import net.flipper.tools.drawtool.collection.util.DrawToolStatusReader

/**
 * Read/write access to the status directories of one collection
 * (`<drawer root>/<serial>`) — the local source of truth per the spec.
 */
class CollectionDrawToolStatusesApi(
    private val collectionPath: Path,
    private val fileSystem: FlipperFileSystem,
    private val statusIdValidator: DrawToolStatusIdValidator,
    private val layout: DrawToolStatusDirectoryLayout,
    private val statusReader: DrawToolStatusReader
) : DrawToolStatusesApi, LogTagProvider {
    override val TAG = "DrawToolLocalCollection"

    override suspend fun getStatusIds(uniqueId: String): CResult<ImmutableList<String>> {
        return runSuspendCatching {
            fileSystem.listOrEmpty(collectionPath)
                .map(Path::name)
                .toImmutableList()
        }.toCResult()
    }

    override suspend fun getStatuses(uniqueId: String): CResult<ImmutableList<DrawToolStatus>> {
        return runSuspendCatching {
            fileSystem.listOrEmpty(collectionPath)
                .filter { childPath -> fileSystem.metadataOrNull(childPath)?.isDirectory == true }
                .map { directoryPath -> directoryPath.name }
                .filter { directoryName -> statusIdValidator.isValid(directoryName) }
                .sorted()
                .mapNotNull { statusId -> statusReader.readDrawToolStatusOrNull(statusId) }
                .sortedWith(
                    comparator = compareByDescending(DrawToolStatus::updatedAt)
                        .thenBy(DrawToolStatus::id)
                )
                .toImmutableList()
        }.toCResult()
    }

    override suspend fun getStatus(
        uniqueId: String,
        statusId: String
    ): CResult<DrawToolStatus?> {
        return runSuspendCatching {
            statusReader.readDrawToolStatusOrNull(statusId)
        }.toCResult()
    }

    override suspend fun deleteStatuses(
        uniqueId: String,
        statusIds: ImmutableList<String>
    ): CResult<Unit> {
        return runSuspendCatching {
            statusIds.forEach { statusId ->
                fileSystem.deleteRecursively(layout.getStatusDirectoryPath(statusId))
                fileSystem.deleteRecursively(layout.getTemporaryDirectoryPath(statusId))
                fileSystem.deleteRecursively(layout.getTrashDirectoryPath(statusId))
            }
        }.toCResult()
    }

    override suspend fun getCollectionPath(uniqueId: String): CResult<Path> {
        return Result.success(collectionPath).toCResult()
    }

    override suspend fun getLayout(uniqueId: String): CResult<DrawToolStatusDirectoryLayout> {
        return CResult.success(layout)
    }
}
