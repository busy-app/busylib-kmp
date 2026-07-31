package net.flipper.tools.drawtool.sync.execute

import dev.zacsweers.metro.Inject
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.ktx.io.FlipperFileSystem
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.storage.di.ClientFileSystemQualifier
import net.flipper.tools.drawtool.sync.model.DrawToolStatusName
import net.flipper.tools.drawtool.sync.storage.DrawToolSyncStateRepository

/**
 * Erases statuses the sync recognized as deleted on a bar. The tombstone is
 * recorded before the file is touched, so a deletion can never outrun it.
 */
@Inject
class DrawToolLocalStatusEraser(
    private val stateRepository: DrawToolSyncStateRepository,
    @ClientFileSystemQualifier
    private val systemFileSystem: FlipperFileSystem,
) : LogTagProvider {
    override val TAG = "DrawToolLocalStatusEraser"

    private suspend fun erase(
        localLayout: DrawToolStatusDirectoryLayout,
        name: DrawToolStatusName,
    ): Result<Unit> {
        return runSuspendCatching {
            stateRepository.recordTombstones(listOf(name))
            systemFileSystem.delete(
                path = localLayout.getStatusFilePath(name.value),
                mustExist = false
            )
        }.onFailure { throwable ->
            error(throwable) { "#erase failed for ${name.value}" }
        }
    }

    suspend fun eraseAll(
        localLayout: DrawToolStatusDirectoryLayout,
        names: Collection<DrawToolStatusName>,
    ): List<Result<Unit>> {
        return names.map { name -> erase(localLayout, name) }
    }
}
