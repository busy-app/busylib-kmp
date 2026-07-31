package net.flipper.tools.drawtool.api

import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.tools.drawtool.api.model.DrawToolSyncException
import net.flipper.tools.drawtool.api.model.DrawToolSyncState

/**
 * Mirrors the local status collection and the collection of the connected bar;
 * the client is the hub between bars.
 *
 * Statuses are compared by name only — the bar exposes neither hashes nor
 * modification times. A name on both sides is left untouched; a name the other
 * side never had is copied over. Deletions propagate to every bar and are
 * permanent, with one exception: a bar whose whole collection is gone is
 * re-filled as fresh, not read as "everything was deleted".
 */
interface DrawToolSyncApi {
    /**
     * Runs one pass against the connected bar; concurrent calls wait their
     * turn. Expected failures come as [DrawToolSyncException] inside
     * `CResult.Failure`; a pass that moved some files and failed on others
     * keeps what it moved.
     */
    suspend fun sync(): CResult<Unit>

    val state: WrappedStateFlow<DrawToolSyncState>
}
