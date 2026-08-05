package net.flipper.tools.drawtool.sync.storage

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.tools.drawtool.sync.model.DrawToolStatusName
import net.flipper.tools.drawtool.sync.model.DrawToolSyncStateSnapshot

/** Sync-memory mutations over [DrawToolSyncStateKrate]; every one is atomic. */
@SingleIn(BusyLibGraph::class)
@Inject
class DrawToolSyncStateRepository(
    private val krate: DrawToolSyncStateKrate,
) {
    suspend fun getSnapshot(): DrawToolSyncStateSnapshot {
        return krate.getValue()
    }

    /**
     * Marks [names] as confirmed present on the bar [serialNumber]. Call only
     * after the transfer is confirmed — recording earlier turns a crashed
     * upload into a false "deleted on the bar" on the next pass.
     */
    suspend fun markSynced(serialNumber: String, names: Collection<DrawToolStatusName>) {
        if (names.isEmpty()) return
        krate.save { snapshot ->
            val synced = snapshot.syncedBySerial[serialNumber].orEmpty() + names
            snapshot.copy(syncedBySerial = snapshot.syncedBySerial + (serialNumber to synced))
        }
    }

    /**
     * Records [names] as deleted, everywhere and forever. They also stop
     * counting as synchronized with any bar: a bar still listing such a name
     * gets it deleted instead of compared.
     */
    suspend fun recordTombstones(names: Collection<DrawToolStatusName>) {
        if (names.isEmpty()) return
        krate.save { snapshot ->
            DrawToolSyncStateSnapshot(
                syncedBySerial = snapshot.syncedBySerial
                    .mapValues { (_, synced) -> synced - names.toSet() },
                tombstones = snapshot.tombstones + names,
            )
        }
    }

    /**
     * Forgets everything recorded for the bar [serialNumber] — for a bar whose
     * collection is gone as a whole and is about to be re-filled as fresh.
     */
    suspend fun forgetBar(serialNumber: String) {
        krate.save { snapshot ->
            snapshot.copy(syncedBySerial = snapshot.syncedBySerial - serialNumber)
        }
    }
}
