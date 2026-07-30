package net.flipper.tools.drawtool.api

import net.flipper.bridge.connection.feature.drawtool.api.model.DrawToolDisplaySide
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.tools.drawtool.api.model.DrawToolDirectoryContents
import net.flipper.tools.drawtool.api.model.DrawToolStoredFile

/**
 * One Draw tool collection. Reads and deletes are local; `upload*` and `show*`
 * need a connected bar.
 */
interface DrawToolStatusesApi {
    /**
     * Every valid status of the collection, newest first
     */
    suspend fun getDrawToolDirectoryContents(): CResult<DrawToolDirectoryContents>

    /**
     * Deletes [files] from the local collection. Absent statuses are
     * skipped silently.
     */
    suspend fun deleteStatuses(files: List<DrawToolStoredFile.Status>): CResult<Unit>

    /**
     * Streams the preview into the bar collection at
     * [DrawToolStatusDirectoryLayout.BUSYBAR_DRAWTOOL_PATH].
     *
     * Fails without a connected bar or without a preview.
     */
    suspend fun uploadPreview(): CResult<Unit>

    /**
     * [uploadPreview] for a status of this collection. [file] keeps its
     * timestamped name on the bar.
     */
    suspend fun uploadStatus(file: DrawToolStoredFile.Status): CResult<Unit>

    /**
     * Draws the preview of the bar collection on [displaySide], transferring
     * nothing: [uploadPreview] has to have put it there.
     *
     * Fails without a connected bar, without an uploaded preview, or during a
     * work session — its screen outranks the drawing.
     */
    suspend fun showPreview(displaySide: DrawToolDisplaySide): CResult<Unit>

    /** [showPreview] for a [file] [uploadStatus] has put on the bar. */
    suspend fun showStatus(
        file: DrawToolStoredFile.Status,
        displaySide: DrawToolDisplaySide
    ): CResult<Unit>

    /**
     * Path layout of the collection of this bar, for callers that address status
     * files themselves instead of hardcoding the directory structure.
     */
    suspend fun getLayout(): CResult<DrawToolStatusDirectoryLayout>
}
