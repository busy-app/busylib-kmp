package net.flipper.tools.drawtool.sync.plan

import dev.zacsweers.metro.Inject
import kotlinx.io.files.Path
import net.flipper.core.busylib.ktx.common.listOrEmpty
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.ktx.io.FlipperFileSystem
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.sync.model.DrawToolStatusName
import net.flipper.tools.drawtool.sync.model.DrawToolSyncPlan
import net.flipper.tools.drawtool.sync.model.DrawToolSyncTarget
import net.flipper.tools.drawtool.sync.storage.DrawToolSyncStateRepository

/** Reads both collections and the sync memory into one [DrawToolSyncPlan]. */
@Inject
class DrawToolSyncPlanFactory(
    private val planner: DrawToolSyncPlanner,
    private val stateRepository: DrawToolSyncStateRepository,
    private val systemFileSystem: FlipperFileSystem,
) : LogTagProvider {
    override val TAG = "DrawToolSyncPlanFactory"

    private suspend fun listStatusNames(
        fileSystem: FlipperFileSystem,
        directory: Path,
    ): Set<DrawToolStatusName> {
        return fileSystem.listOrEmpty(directory)
            .map { path -> path.name }
            .filter { name -> DrawToolStatusDirectoryLayout.STATUS_FILE_REGEX.matches(name) }
            .map(::DrawToolStatusName)
            .toSet()
    }

    /**
     * An unreachable bar and a reset bar both list as empty, and an empty
     * listing is what turns remembered deletions into a re-fill. Only a
     * successful write proves the emptiness is real: creating the collection
     * throws on an unreachable bar and is idempotent on a reset one.
     */
    private suspend fun listBarStatusNames(target: DrawToolSyncTarget): Set<DrawToolStatusName> {
        val barCollectionPath = target.barLayout.getCollectionPath()
        val names = listStatusNames(target.barFileSystem, barCollectionPath)
        if (names.isNotEmpty()) return names
        target.barFileSystem.createDirectories(barCollectionPath, mustCreate = false)
        return listStatusNames(target.barFileSystem, barCollectionPath)
    }

    suspend fun create(
        target: DrawToolSyncTarget,
        localLayout: DrawToolStatusDirectoryLayout,
    ): Result<DrawToolSyncPlan> {
        return runSuspendCatching {
            val snapshot = stateRepository.getSnapshot()
            planner.plan(
                localNames = listStatusNames(systemFileSystem, localLayout.getCollectionPath()),
                barNames = listBarStatusNames(target),
                syncedWithBar = snapshot.syncedBySerial[target.serialNumber].orEmpty(),
                tombstones = snapshot.tombstones,
            )
        }.onSuccess { plan ->
            info { "#create for ${target.serialNumber}: $plan" }
        }
    }
}
