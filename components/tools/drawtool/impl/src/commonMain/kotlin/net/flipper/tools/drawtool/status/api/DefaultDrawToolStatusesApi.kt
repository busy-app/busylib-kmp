package net.flipper.tools.drawtool.status.api

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import net.flipper.bridge.connection.feature.drawtool.api.FDrawToolFeatureApi
import net.flipper.bridge.connection.feature.drawtool.api.model.DrawToolDisplaySide
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.getSync
import net.flipper.bridge.connection.feature.storage.api.FStorageFeatureApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.core.busylib.ktx.common.copyFileTo
import net.flipper.core.busylib.ktx.common.mapSuspendCatching
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.ktx.io.FlipperFileSystem
import net.flipper.core.busylib.ktx.io.SystemFlipperFileSystem
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.api.DrawToolStatusesApi
import net.flipper.tools.drawtool.api.model.DrawToolDirectoryContents
import net.flipper.tools.drawtool.api.model.DrawToolStoredFile
import net.flipper.tools.drawtool.layout.api.DefaultDrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.status.util.DrawToolStoredFileResolver
import net.flipper.tools.drawtool.storage.api.DrawToolStoragePathProvider

@SingleIn(BusyLibGraph::class)
@Inject
@ContributesBinding(BusyLibGraph::class, binding<DrawToolStatusesApi>())
class DefaultDrawToolStatusesApi(
    private val drawToolStoragePathProvider: DrawToolStoragePathProvider,
    private val drawToolStoredFileResolver: DrawToolStoredFileResolver,
    private val featureProvider: FFeatureProvider,
    private val systemFileSystem: FlipperFileSystem = SystemFlipperFileSystem(SystemFileSystem),
) : DrawToolStatusesApi {
    private val mutex = Mutex()

    private val barLayout = DefaultDrawToolStatusDirectoryLayout(
        DrawToolStatusDirectoryLayout.BUSYBAR_DRAWTOOL_PATH
    )

    override suspend fun getDrawToolDirectoryContents(): CResult<DrawToolDirectoryContents> {
        return mutex.withLock {
            drawToolStoragePathProvider.getPath()
                .mapSuspendCatching(systemFileSystem::list)
                .map { paths ->
                    val drawToolStoredFiles = paths
                        .mapNotNull { path -> drawToolStoredFileResolver.resolve(path) }
                        .sortedByDescending { storedFile -> storedFile.path.name }
                    DrawToolDirectoryContents(drawToolStoredFiles)
                }
                .toCResult()
        }
    }

    override suspend fun deleteStatuses(files: List<DrawToolStoredFile.Status>): CResult<Unit> {
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

    private suspend fun uploadToBarUnsafe(file: DrawToolStoredFile.Status) {
        val busyBarFileSystem: FlipperFileSystem = requireNotNull(
            value = featureProvider.getSync<FStorageFeatureApi>(),
            lazyMessage = { "FStorageFeatureApi feature is unavailable, the bar is most likely not connected" }
        )
        busyBarFileSystem.createDirectories(
            path = DrawToolStatusDirectoryLayout.BUSYBAR_DRAWTOOL_PATH,
            mustCreate = false
        )
        systemFileSystem.copyFileTo(
            sourcePath = file.path,
            destination = busyBarFileSystem,
            destinationPath = barLayout.getStoredFilePath(file)
        )
    }

    private suspend fun drawOnBarUnsafe(filePath: Path, displaySide: DrawToolDisplaySide) {
        val drawToolFeatureApi = requireNotNull(
            value = featureProvider.getSync<FDrawToolFeatureApi>(),
            lazyMessage = { "FDrawToolFeatureApi feature is unavailable, the bar is most likely not connected" }
        )
        drawToolFeatureApi.showFile(filePath, displaySide).getOrThrow()
    }

    override suspend fun uploadStatus(file: DrawToolStoredFile.Status): CResult<Unit> {
        return mutex.withLock {
            runSuspendCatching {
                uploadToBarUnsafe(file)
            }.toCResult()
        }
    }

    override suspend fun showStatus(
        file: DrawToolStoredFile.Status,
        displaySide: DrawToolDisplaySide
    ): CResult<Unit> {
        return mutex.withLock {
            runSuspendCatching {
                drawOnBarUnsafe(barLayout.getStoredFilePath(file), displaySide)
            }.toCResult()
        }
    }
}
