package net.flipper.tools.drawtool.sync.storage

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.flipper.tools.drawtool.sync.model.DrawToolStatusName
import net.flipper.tools.drawtool.sync.model.DrawToolSyncStateSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DrawToolSyncStateRepositoryTest {

    private fun createRepository(settings: MapSettings): DrawToolSyncStateRepository {
        return DrawToolSyncStateRepository(DrawToolSyncStateKrateImpl(settings, Json))
    }

    private fun statusNames(vararg values: String): List<DrawToolStatusName> {
        return values.map(::DrawToolStatusName)
    }

    @Test
    fun GIVEN_fresh_storage_WHEN_snapshot_read_THEN_snapshot_is_empty() = runTest {
        val repository = createRepository(MapSettings())

        assertEquals(DrawToolSyncStateSnapshot(), repository.getSnapshot())
    }

    @Test
    fun GIVEN_marks_on_separate_passes_WHEN_snapshot_read_THEN_names_accumulate() = runTest {
        val repository = createRepository(MapSettings())

        repository.markSynced(FIRST_SERIAL, statusNames("a.png"))
        repository.markSynced(FIRST_SERIAL, statusNames("b.png"))

        val snapshot = repository.getSnapshot()
        assertEquals(statusNames("a.png", "b.png").toSet(), snapshot.syncedBySerial[FIRST_SERIAL])
    }

    @Test
    fun GIVEN_name_synced_with_two_bars_WHEN_tombstoned_THEN_removed_from_every_bar_forever() = runTest {
        val repository = createRepository(MapSettings())
        repository.markSynced(FIRST_SERIAL, statusNames("dead.png", "alive.png"))
        repository.markSynced(SECOND_SERIAL, statusNames("dead.png"))

        repository.recordTombstones(statusNames("dead.png"))

        val snapshot = repository.getSnapshot()
        assertEquals(statusNames("dead.png").toSet(), snapshot.tombstones)
        assertEquals(statusNames("alive.png").toSet(), snapshot.syncedBySerial[FIRST_SERIAL])
        assertEquals(emptySet(), snapshot.syncedBySerial[SECOND_SERIAL])
    }

    @Test
    fun GIVEN_two_bars_remembered_WHEN_one_forgotten_THEN_other_bar_and_tombstones_survive() = runTest {
        val repository = createRepository(MapSettings())
        repository.markSynced(FIRST_SERIAL, statusNames("a.png"))
        repository.markSynced(SECOND_SERIAL, statusNames("b.png"))
        repository.recordTombstones(statusNames("dead.png"))

        repository.forgetBar(FIRST_SERIAL)

        val snapshot = repository.getSnapshot()
        assertNull(snapshot.syncedBySerial[FIRST_SERIAL])
        assertEquals(statusNames("b.png").toSet(), snapshot.syncedBySerial[SECOND_SERIAL])
        assertEquals(statusNames("dead.png").toSet(), snapshot.tombstones)
    }

    @Test
    fun GIVEN_empty_name_lists_WHEN_marked_or_tombstoned_THEN_nothing_is_written() = runTest {
        val settings = MapSettings()
        val repository = createRepository(settings)

        repository.markSynced(FIRST_SERIAL, emptyList())
        repository.recordTombstones(emptyList())

        assertEquals(0, settings.size)
    }

    @Test
    fun GIVEN_corrupted_stored_state_WHEN_snapshot_read_THEN_degrades_to_empty() = runTest {
        val settings = MapSettings()
        settings.putString(STORAGE_KEY, "not a json at all")
        val repository = createRepository(settings)

        assertEquals(DrawToolSyncStateSnapshot(), repository.getSnapshot())
    }

    @Test
    fun GIVEN_recorded_state_WHEN_reopened_over_the_same_settings_THEN_state_survives() = runTest {
        val settings = MapSettings()
        val repository = createRepository(settings)
        repository.markSynced(FIRST_SERIAL, statusNames("a.png"))
        repository.recordTombstones(statusNames("dead.png"))

        val reopenedSnapshot = createRepository(settings).getSnapshot()

        assertEquals(statusNames("a.png").toSet(), reopenedSnapshot.syncedBySerial[FIRST_SERIAL])
        assertEquals(statusNames("dead.png").toSet(), reopenedSnapshot.tombstones)
    }

    @Test
    fun GIVEN_concurrent_marks_and_tombstones_WHEN_all_complete_THEN_no_update_is_lost() = runTest {
        val repository = createRepository(MapSettings())
        val syncedNames = List(CONCURRENCY) { index -> DrawToolStatusName("synced_$index.png") }
        val tombstonedNames = List(CONCURRENCY) { index -> DrawToolStatusName("dead_$index.png") }

        coroutineScope {
            syncedNames.forEach { name ->
                launch { repository.markSynced(FIRST_SERIAL, listOf(name)) }
            }
            tombstonedNames.forEach { name ->
                launch { repository.recordTombstones(listOf(name)) }
            }
        }

        val snapshot = repository.getSnapshot()
        assertEquals(syncedNames.toSet(), snapshot.syncedBySerial[FIRST_SERIAL])
        assertEquals(tombstonedNames.toSet(), snapshot.tombstones)
    }

    companion object {
        private const val FIRST_SERIAL = "bar-serial-1"
        private const val SECOND_SERIAL = "bar-serial-2"
        private const val STORAGE_KEY = "draw_tool_sync_state"
        private const val CONCURRENCY = 50
    }
}
