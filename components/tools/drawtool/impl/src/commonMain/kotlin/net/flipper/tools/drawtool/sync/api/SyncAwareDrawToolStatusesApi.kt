package net.flipper.tools.drawtool.sync.api

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.ktx.common.transform
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.api.DrawToolStatusesApi
import net.flipper.tools.drawtool.api.model.DrawToolStoredFile
import net.flipper.tools.drawtool.status.api.DefaultDrawToolStatusesApi
import net.flipper.tools.drawtool.sync.model.DrawToolStatusName
import net.flipper.tools.drawtool.sync.storage.DrawToolSyncStateRepository
import net.flipper.tools.drawtool.sync.trigger.DrawToolCollectionEventProducer

/**
 * The graph-bound [DrawToolStatusesApi]: adds the sync bookkeeping a deletion
 * owes. The tombstone is recorded before the file is touched, so a deletion
 * can never outrun it — and a deletion whose tombstone cannot be recorded is
 * refused, because the next sync pass would bring the files back. A wrapper
 * because [DefaultDrawToolStatusesApi] is also constructed standalone against
 * other roots and must stay free of sync state.
 */
@SingleIn(BusyLibGraph::class)
@Inject
@ContributesBinding(BusyLibGraph::class, binding<DrawToolStatusesApi>())
class SyncAwareDrawToolStatusesApi(
    private val delegate: DefaultDrawToolStatusesApi,
    private val stateRepository: DrawToolSyncStateRepository,
    private val collectionEvents: DrawToolCollectionEventProducer,
) : DrawToolStatusesApi by delegate {
    override suspend fun deleteStatuses(files: List<DrawToolStoredFile.Status>): CResult<Unit> {
        val statusNames = files
            .map { storedFile -> storedFile.path.name }
            .filter { name ->
                DrawToolStatusDirectoryLayout.STATUS_FILE_BROKEN_REGEX.matches(name)
                    .or(DrawToolStatusDirectoryLayout.STATUS_FILE_REGEX.matches(name))
            }
            .map(::DrawToolStatusName)
        return runSuspendCatching { stateRepository.recordTombstones(statusNames) }
            .transform { _ -> delegate.deleteStatuses(files).toKotlinResult() }
            .toCResult()
            .onSuccess { _ -> collectionEvents.notifyChanged() }
    }
}
