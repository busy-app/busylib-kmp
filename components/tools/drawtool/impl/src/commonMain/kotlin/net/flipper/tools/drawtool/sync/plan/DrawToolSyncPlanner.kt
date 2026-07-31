package net.flipper.tools.drawtool.sync.plan

import dev.zacsweers.metro.Inject
import net.flipper.tools.drawtool.sync.model.DrawToolStatusName
import net.flipper.tools.drawtool.sync.model.DrawToolSyncPlan

/**
 * The sync decision table. For every status name the four facts — on the
 * client, on the bar, remembered as synced with this bar, tombstoned — decide
 * one action:
 *
 * - on both → leave alone, remember as synced;
 * - tombstoned → delete wherever it still is, never transfer;
 * - only local, bar never had it → upload;
 * - only local, bar had it → it was deleted on the bar → tombstone and delete;
 * - only on bar → download, even when remembered as synced: local deletions
 *   always leave a tombstone, so a missing local file without one is local
 *   data loss, and the bar copy restores it.
 *
 * The one exception: a bar that lists nothing although statuses were
 * synchronized with it lost its whole collection (reset, wiped storage) and is
 * re-filled as fresh instead of being read as "everything was deleted".
 */
@Inject
class DrawToolSyncPlanner {
    fun plan(
        localNames: Set<DrawToolStatusName>,
        barNames: Set<DrawToolStatusName>,
        syncedWithBar: Set<DrawToolStatusName>,
        tombstones: Set<DrawToolStatusName>,
    ): DrawToolSyncPlan {
        val isBarReset = barNames.isEmpty() && syncedWithBar.isNotEmpty()
        val rememberedOnBar = if (isBarReset) emptySet() else syncedWithBar

        val liveLocalNames = localNames - tombstones
        val localOnlyNames = liveLocalNames - barNames
        val deletedOnBar = localOnlyNames intersect rememberedOnBar

        return DrawToolSyncPlan(
            uploadToBar = localOnlyNames - deletedOnBar,
            downloadFromBar = barNames - localNames - tombstones,
            deleteLocally = deletedOnBar + (localNames intersect tombstones),
            deleteFromBar = barNames intersect tombstones,
            markInSync = liveLocalNames intersect barNames,
            isBarReset = isBarReset,
        )
    }
}
