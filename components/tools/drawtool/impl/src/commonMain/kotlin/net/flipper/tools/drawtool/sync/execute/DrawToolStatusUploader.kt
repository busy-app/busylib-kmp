package net.flipper.tools.drawtool.sync.execute

import dev.zacsweers.metro.Inject
import net.flipper.core.busylib.ktx.common.mapSuspendCatching
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.ktx.io.FlipperFileSystem
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.storage.di.ClientFileSystemQualifier
import net.flipper.tools.drawtool.sync.model.DrawToolStatusName
import net.flipper.tools.drawtool.sync.model.DrawToolSyncTarget
import net.flipper.tools.drawtool.sync.storage.DrawToolSyncStateRepository

/** Brings local statuses into the bar collection, one atomic file at a time. */
@Inject
class DrawToolStatusUploader(
    private val fileTransfer: DrawToolAtomicFileTransfer,
    private val stateRepository: DrawToolSyncStateRepository,
    @ClientFileSystemQualifier private val systemFileSystem: FlipperFileSystem,
) : LogTagProvider {
    override val TAG = "DrawToolStatusUploader"

    private suspend fun upload(
        target: DrawToolSyncTarget,
        localLayout: DrawToolStatusDirectoryLayout,
        name: DrawToolStatusName,
    ): Result<Unit> {
        return fileTransfer.transfer(
            source = systemFileSystem,
            sourcePath = localLayout.getStatusFilePath(name.value),
            destination = target.barFileSystem,
            temporaryPath = target.barLayout.getTemporaryFilePath(),
            destinationPath = target.barLayout.getStatusFilePath(name.value),
        ).mapSuspendCatching {
            stateRepository.markSynced(target.serialNumber, listOf(name))
        }.onFailure { throwable ->
            error(throwable) { "#upload failed for ${name.value}" }
        }
    }

    suspend fun uploadAll(
        target: DrawToolSyncTarget,
        localLayout: DrawToolStatusDirectoryLayout,
        names: Collection<DrawToolStatusName>,
    ): List<Result<Unit>> {
        if (names.isEmpty()) return emptyList()
        return runSuspendCatching {
            target.barFileSystem.createDirectories(
                path = target.barLayout.getCollectionPath(),
                mustCreate = false
            )
        }.fold(
            onFailure = { throwable -> names.map { _ -> Result.failure(throwable) } },
            onSuccess = { names.map { name -> upload(target, localLayout, name) } },
        )
    }
}
