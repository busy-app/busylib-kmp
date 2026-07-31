package net.flipper.tools.drawtool.sync.execute

import dev.zacsweers.metro.Inject
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.ktx.io.FlipperFileSystem
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.sync.model.DrawToolSyncTarget

/**
 * Removes transfer leftovers a crash cut short. Best effort: a leftover is
 * invisible to listings anyway and only wastes space.
 */
@Inject
class DrawToolSyncTemporaryCleaner(
    private val systemFileSystem: FlipperFileSystem,
) {
    suspend fun cleanup(target: DrawToolSyncTarget, localLayout: DrawToolStatusDirectoryLayout) {
        runSuspendCatching {
            systemFileSystem.delete(localLayout.getTemporaryFilePath(), mustExist = false)
        }
        runSuspendCatching {
            target.barFileSystem.delete(target.barLayout.getTemporaryFilePath(), mustExist = false)
        }
    }
}
