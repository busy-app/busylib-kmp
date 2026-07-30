package net.flipper.tools.drawtool.storage

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.io.files.Path
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.tools.drawtool.storage.api.DrawToolStoragePathProvider

/** Draw tool root in the home directory of the user, as `.busylib/draw_tool`. */
@Inject
@ContributesBinding(BusyLibGraph::class, binding<DrawToolStoragePathProvider>())
class JvmDrawToolStoragePathProvider : DrawToolStoragePathProvider {
    override fun getPath(): Result<Path> {
        val userHome = System.getProperty("user.home")
        return if (userHome.isNullOrBlank()) {
            Result.failure(IllegalStateException("The user.home system property is not set"))
        } else {
            Result.success(Path(userHome, ".busylib", "draw_tool"))
        }
    }
}
