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

/** Brings bar statuses into the local collection, one atomic file at a time. */
@Inject
class DrawToolStatusDownloader(
    private val fileTransfer: DrawToolAtomicFileTransfer,
    private val stateRepository: DrawToolSyncStateRepository,
    @ClientFileSystemQualifier private val systemFileSystem: FlipperFileSystem,
) : LogTagProvider {
    override val TAG = "DrawToolStatusDownloader"

    private suspend fun download(
        target: DrawToolSyncTarget,
        localLayout: DrawToolStatusDirectoryLayout,
        name: DrawToolStatusName,
    ): Result<Unit> {
        return fileTransfer.transfer(
            source = target.barFileSystem,
            sourcePath = target.barLayout.getStatusFilePath(name.value),
            destination = systemFileSystem,
            temporaryPath = localLayout.getTemporaryFilePath(),
            destinationPath = localLayout.getStatusFilePath(name.value),
        ).mapSuspendCatching {
            stateRepository.markSynced(target.serialNumber, listOf(name))
        }.onFailure { throwable ->
            error(throwable) { "#download failed for ${name.value}" }
        }
    }

    suspend fun downloadAll(
        target: DrawToolSyncTarget,
        localLayout: DrawToolStatusDirectoryLayout,
        names: Collection<DrawToolStatusName>,
    ): List<Result<Unit>> {
        if (names.isEmpty()) return emptyList()
        return runSuspendCatching {
            systemFileSystem.createDirectories(localLayout.getCollectionPath(), mustCreate = false)
        }.fold(
            onFailure = { throwable -> names.map { _ -> Result.failure(throwable) } },
            onSuccess = { names.map { name -> download(target, localLayout, name) } },
        )
    }
}
