package net.flipper.tools.drawtool.status.api

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.files.SystemFileSystem
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.core.busylib.ktx.common.mapSuspendCatching
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.ktx.io.FlipperFileSystem
import net.flipper.core.busylib.ktx.io.SystemFlipperFileSystem
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.api.DrawToolStatusesApi
import net.flipper.tools.drawtool.api.model.DrawToolDirectoryContents
import net.flipper.tools.drawtool.api.model.DrawToolStoredFile
import net.flipper.tools.drawtool.layout.api.DefaultDrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.status.util.DrawToolFileTypeResolver
import net.flipper.tools.drawtool.storage.api.DrawToolStoragePathProvider

@SingleIn(BusyLibGraph::class)
@Inject
@ContributesBinding(BusyLibGraph::class, binding<DrawToolStatusesApi>())
class DefaultDrawToolStatusesApi(
    private val drawToolStoragePathProvider: DrawToolStoragePathProvider,
    private val drawToolFileTypeResolver: DrawToolFileTypeResolver,
    private val systemFileSystem: FlipperFileSystem = SystemFlipperFileSystem(SystemFileSystem),
) : DrawToolStatusesApi {
    private val mutex = Mutex()

    override suspend fun getStatuses(): CResult<DrawToolDirectoryContents> {
        return mutex.withLock {
            drawToolStoragePathProvider.getPath()
                .mapSuspendCatching(systemFileSystem::list)
                .map { paths ->
                    val drawToolStoredFiles = paths.mapNotNull { path ->
                        val type = drawToolFileTypeResolver
                            .resolve(path)
                            ?: return@mapNotNull null
                        DrawToolStoredFile(
                            type = type,
                            path = path
                        )
                    }
                    DrawToolDirectoryContents(drawToolStoredFiles)
                }
                .toCResult()
        }
    }

    override suspend fun deleteStatuses(files: List<DrawToolStoredFile>): CResult<Unit> {
        return mutex.withLock {
            runSuspendCatching {
                files.forEach { drawToolStoredFile ->
                    systemFileSystem.delete(drawToolStoredFile.path, false)
                }
            }.toCResult()
        }
    }

    override suspend fun getLayout(): CResult<DrawToolStatusDirectoryLayout> {
        return drawToolStoragePathProvider.getPath()
            .map(::DefaultDrawToolStatusDirectoryLayout)
            .toCResult()
    }
}
