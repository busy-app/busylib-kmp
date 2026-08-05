package net.flipper.tools.drawtool.sync.plan

import net.flipper.tools.drawtool.sync.model.DrawToolStatusName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DrawToolSyncPlannerTest {

    private val planner = DrawToolSyncPlanner()

    private fun statusNames(vararg values: String): Set<DrawToolStatusName> {
        return values.map(::DrawToolStatusName).toSet()
    }

    @Test
    fun GIVEN_everything_empty_WHEN_planned_THEN_plan_is_empty() {
        val plan = planner.plan(
            localNames = emptySet(),
            barNames = emptySet(),
            syncedWithBar = emptySet(),
            tombstones = emptySet(),
        )

        assertEquals(emptySet(), plan.uploadToBar)
        assertEquals(emptySet(), plan.downloadFromBar)
        assertEquals(emptySet(), plan.deleteLocally)
        assertEquals(emptySet(), plan.deleteFromBar)
        assertEquals(emptySet(), plan.markInSync)
        assertFalse(plan.isBarReset)
    }

    @Test
    fun GIVEN_name_on_both_sides_WHEN_planned_THEN_only_marked_in_sync() {
        val plan = planner.plan(
            localNames = statusNames("a.png"),
            barNames = statusNames("a.png"),
            syncedWithBar = emptySet(),
            tombstones = emptySet(),
        )

        assertEquals(statusNames("a.png"), plan.markInSync)
        assertEquals(emptySet(), plan.uploadToBar)
        assertEquals(emptySet(), plan.downloadFromBar)
        assertEquals(emptySet(), plan.deleteLocally)
        assertEquals(emptySet(), plan.deleteFromBar)
    }

    @Test
    fun GIVEN_local_name_the_bar_never_had_WHEN_planned_THEN_uploaded() {
        val plan = planner.plan(
            localNames = statusNames("new.png"),
            barNames = emptySet(),
            syncedWithBar = emptySet(),
            tombstones = emptySet(),
        )

        assertEquals(statusNames("new.png"), plan.uploadToBar)
        assertEquals(emptySet(), plan.deleteLocally)
        assertFalse(plan.isBarReset)
    }

    @Test
    fun GIVEN_local_name_deleted_on_the_bar_WHEN_planned_THEN_deleted_locally_not_reuploaded() {
        val plan = planner.plan(
            localNames = statusNames("kept.png", "deleted_on_bar.png"),
            barNames = statusNames("kept.png"),
            syncedWithBar = statusNames("kept.png", "deleted_on_bar.png"),
            tombstones = emptySet(),
        )

        assertEquals(statusNames("deleted_on_bar.png"), plan.deleteLocally)
        assertEquals(emptySet(), plan.uploadToBar)
        assertEquals(statusNames("kept.png"), plan.markInSync)
    }

    @Test
    fun GIVEN_bar_only_name_WHEN_planned_THEN_downloaded() {
        val plan = planner.plan(
            localNames = emptySet(),
            barNames = statusNames("on_bar.png"),
            syncedWithBar = emptySet(),
            tombstones = emptySet(),
        )

        assertEquals(statusNames("on_bar.png"), plan.downloadFromBar)
        assertEquals(emptySet(), plan.deleteFromBar)
    }

    @Test
    fun GIVEN_local_file_lost_without_tombstone_WHEN_planned_THEN_restored_from_bar() {
        // A missing local file that was never tombstoned is local data loss,
        // not a deletion: deletions always tombstone first.
        val plan = planner.plan(
            localNames = emptySet(),
            barNames = statusNames("lost.png"),
            syncedWithBar = statusNames("lost.png"),
            tombstones = emptySet(),
        )

        assertEquals(statusNames("lost.png"), plan.downloadFromBar)
        assertEquals(emptySet(), plan.deleteFromBar)
    }

    @Test
    fun GIVEN_tombstoned_name_still_on_bar_WHEN_planned_THEN_deleted_from_bar_never_downloaded() {
        val plan = planner.plan(
            localNames = emptySet(),
            barNames = statusNames("dead.png"),
            syncedWithBar = emptySet(),
            tombstones = statusNames("dead.png"),
        )

        assertEquals(statusNames("dead.png"), plan.deleteFromBar)
        assertEquals(emptySet(), plan.downloadFromBar)
        assertEquals(emptySet(), plan.markInSync)
    }

    @Test
    fun GIVEN_tombstoned_name_still_local_WHEN_planned_THEN_deleted_locally_never_uploaded() {
        val plan = planner.plan(
            localNames = statusNames("dead.png"),
            barNames = emptySet(),
            syncedWithBar = emptySet(),
            tombstones = statusNames("dead.png"),
        )

        assertEquals(statusNames("dead.png"), plan.deleteLocally)
        assertEquals(emptySet(), plan.uploadToBar)
    }

    @Test
    fun GIVEN_tombstoned_name_on_both_sides_WHEN_planned_THEN_deleted_everywhere() {
        val plan = planner.plan(
            localNames = statusNames("dead.png"),
            barNames = statusNames("dead.png"),
            syncedWithBar = statusNames("dead.png"),
            tombstones = statusNames("dead.png"),
        )

        assertEquals(statusNames("dead.png"), plan.deleteLocally)
        assertEquals(statusNames("dead.png"), plan.deleteFromBar)
        assertEquals(emptySet(), plan.markInSync)
        assertEquals(emptySet(), plan.uploadToBar)
        assertEquals(emptySet(), plan.downloadFromBar)
    }

    @Test
    fun GIVEN_empty_bar_with_sync_memory_WHEN_planned_THEN_reset_and_refilled() {
        // The bar lost its whole collection: re-fill it instead of reading the
        // emptiness as "everything was deleted on the bar".
        val plan = planner.plan(
            localNames = statusNames("a.png", "b.png"),
            barNames = emptySet(),
            syncedWithBar = statusNames("a.png", "b.png"),
            tombstones = emptySet(),
        )

        assertTrue(plan.isBarReset)
        assertEquals(statusNames("a.png", "b.png"), plan.uploadToBar)
        assertEquals(emptySet(), plan.deleteLocally)
    }

    @Test
    fun GIVEN_empty_bar_without_sync_memory_WHEN_planned_THEN_fresh_upload_without_reset() {
        val plan = planner.plan(
            localNames = statusNames("a.png"),
            barNames = emptySet(),
            syncedWithBar = emptySet(),
            tombstones = emptySet(),
        )

        assertFalse(plan.isBarReset)
        assertEquals(statusNames("a.png"), plan.uploadToBar)
    }

    @Test
    fun GIVEN_bar_reset_with_tombstoned_local_names_WHEN_planned_THEN_tombstoned_not_refilled() {
        val plan = planner.plan(
            localNames = statusNames("alive.png", "dead.png"),
            barNames = emptySet(),
            syncedWithBar = statusNames("alive.png"),
            tombstones = statusNames("dead.png"),
        )

        assertTrue(plan.isBarReset)
        assertEquals(statusNames("alive.png"), plan.uploadToBar)
        assertEquals(statusNames("dead.png"), plan.deleteLocally)
    }

    @Test
    fun GIVEN_stale_synced_memory_after_tombstone_race_WHEN_planned_THEN_tombstone_wins() {
        // markSynced confirmed after recordTombstones can leave a name both
        // tombstoned and remembered as synced; the tombstone must win.
        val plan = planner.plan(
            localNames = emptySet(),
            barNames = statusNames("raced.png"),
            syncedWithBar = statusNames("raced.png"),
            tombstones = statusNames("raced.png"),
        )

        assertEquals(statusNames("raced.png"), plan.deleteFromBar)
        assertEquals(emptySet(), plan.downloadFromBar)
    }

    @Test
    fun GIVEN_all_cases_at_once_WHEN_planned_THEN_every_name_gets_exactly_one_action() {
        val plan = planner.plan(
            localNames = statusNames("both.png", "new_local.png", "deleted_on_bar.png", "dead_local.png"),
            barNames = statusNames("both.png", "new_bar.png", "dead_bar.png"),
            syncedWithBar = statusNames("both.png", "deleted_on_bar.png"),
            tombstones = statusNames("dead_local.png", "dead_bar.png"),
        )

        assertEquals(statusNames("new_local.png"), plan.uploadToBar)
        assertEquals(statusNames("new_bar.png"), plan.downloadFromBar)
        assertEquals(statusNames("deleted_on_bar.png", "dead_local.png"), plan.deleteLocally)
        assertEquals(statusNames("dead_bar.png"), plan.deleteFromBar)
        assertEquals(statusNames("both.png"), plan.markInSync)
        assertFalse(plan.isBarReset)
    }
}
