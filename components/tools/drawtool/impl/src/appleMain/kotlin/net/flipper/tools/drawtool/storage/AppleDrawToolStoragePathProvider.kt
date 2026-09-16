package net.flipper.tools.drawtool.storage

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.tools.drawtool.storage.api.DrawToolStoragePathProvider
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** Draw tool root under `Application Support`, which is created if absent. */
@Inject
@ContributesBinding(BusyLibGraph::class, binding<DrawToolStoragePathProvider>())
class AppleDrawToolStoragePathProvider : DrawToolStoragePathProvider {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun getPath(): Result<Path> {
        return withContext(FlipperDispatchers.default) {
            val applicationSupportUrl = NSFileManager.defaultManager.URLForDirectory(
                directory = NSApplicationSupportDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null
            )
            val applicationSupportPath = applicationSupportUrl?.path
                ?: return@withContext Result.failure(
                    IllegalStateException("Cannot resolve the Application Support directory")
                )
            Result.success(Path(applicationSupportPath, "busylib", "draw_tool"))
        }
    }
}
