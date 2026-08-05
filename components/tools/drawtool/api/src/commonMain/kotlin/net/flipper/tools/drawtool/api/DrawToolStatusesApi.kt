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
     * Streams [file] into the bar collection at
     * [DrawToolStatusDirectoryLayout.BUSYBAR_DRAWTOOL_PATH], keeping its
     * timestamped name. Displays nothing.
     *
     * Fails without a connected bar.
     */
    suspend fun uploadStatus(file: DrawToolStoredFile.Status): CResult<Unit>

    /**
     * Draws a [file] [uploadStatus] has put on the bar on [displaySide],
     * transferring nothing.
     *
     * Fails without a connected bar, without the uploaded file, or during a
     * work session — its screen outranks the drawing.
     */
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
