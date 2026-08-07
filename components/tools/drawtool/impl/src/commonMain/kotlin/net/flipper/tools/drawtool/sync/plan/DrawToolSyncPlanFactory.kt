package net.flipper.tools.drawtool.sync.plan

import dev.zacsweers.metro.Inject
import kotlinx.io.files.Path
import net.flipper.core.busylib.ktx.common.listOrEmpty
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.ktx.io.FlipperFileSystem
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.storage.di.ClientFileSystemQualifier
import net.flipper.tools.drawtool.sync.model.DrawToolStatusName
import net.flipper.tools.drawtool.sync.model.DrawToolSyncPlan
import net.flipper.tools.drawtool.sync.model.DrawToolSyncTarget
import net.flipper.tools.drawtool.sync.storage.DrawToolSyncStateRepository

/** Reads both collections and the sync memory into one [DrawToolSyncPlan]. */
@Inject
class DrawToolSyncPlanFactory(
    private val planner: DrawToolSyncPlanner,
    private val stateRepository: DrawToolSyncStateRepository,
    @ClientFileSystemQualifier
    private val systemFileSystem: FlipperFileSystem,
) : LogTagProvider {
    override val TAG = "DrawToolSyncPlanFactory"

    private fun toStatusNames(paths: Collection<Path>): Set<DrawToolStatusName> {
        return paths
            .map { path -> path.name }
            .filter { name ->
                DrawToolStatusDirectoryLayout.STATUS_FILE_BROKEN_REGEX.matches(name)
                    .or(DrawToolStatusDirectoryLayout.STATUS_FILE_REGEX.matches(name))
            }
            .map(::DrawToolStatusName)
            .toSet()
    }

    private suspend fun listLocalStatusNames(
        localLayout: DrawToolStatusDirectoryLayout,
    ): Set<DrawToolStatusName> {
        return toStatusNames(systemFileSystem.listOrEmpty(localLayout.getCollectionPath()))
    }

    /**
     * An unreachable bar and a reset bar can both look empty, and an empty
     * listing is what turns remembered deletions into a re-fill. Two proofs
     * keep them apart: creating the collection throws on an unreachable bar
     * and is idempotent on a reset one, and the strict [FlipperFileSystem.list]
     * throws instead of reading an unlistable directory as empty.
     */
    private suspend fun listBarStatusNames(target: DrawToolSyncTarget): Set<DrawToolStatusName> {
        val barCollectionPath = target.barLayout.getCollectionPath()
        target.barFileSystem.createDirectories(barCollectionPath, mustCreate = false)
        return toStatusNames(target.barFileSystem.list(barCollectionPath))
    }

    suspend fun create(
        target: DrawToolSyncTarget,
        localLayout: DrawToolStatusDirectoryLayout,
    ): Result<DrawToolSyncPlan> {
        return runSuspendCatching {
            val snapshot = stateRepository.getSnapshot()
            planner.plan(
                localNames = listLocalStatusNames(localLayout),
                barNames = listBarStatusNames(target),
                syncedWithBar = snapshot.syncedBySerial[target.serialNumber].orEmpty(),
                tombstones = snapshot.tombstones,
            )
        }.onSuccess { plan ->
            info { "#create for ${target.serialNumber}: $plan" }
        }
    }
}
