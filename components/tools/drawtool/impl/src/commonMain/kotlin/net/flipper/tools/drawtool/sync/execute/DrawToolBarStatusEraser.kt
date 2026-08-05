package net.flipper.tools.drawtool.sync.execute

import dev.zacsweers.metro.Inject
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.tools.drawtool.sync.model.DrawToolStatusName
import net.flipper.tools.drawtool.sync.model.DrawToolSyncTarget

/** Erases tombstoned statuses a bar still lists. */
@Inject
class DrawToolBarStatusEraser : LogTagProvider {
    override val TAG = "DrawToolBarStatusEraser"

    private suspend fun erase(target: DrawToolSyncTarget, name: DrawToolStatusName): Result<Unit> {
        return runSuspendCatching {
            target.barFileSystem.delete(
                path = target.barLayout.getStatusFilePath(name.value),
                mustExist = false
            )
        }.onFailure { throwable ->
            error(throwable) { "#erase failed for ${name.value}" }
        }
    }

    suspend fun eraseAll(
        target: DrawToolSyncTarget,
        names: Collection<DrawToolStatusName>,
    ): List<Result<Unit>> {
        return names.map { name -> erase(target, name) }
    }
}
