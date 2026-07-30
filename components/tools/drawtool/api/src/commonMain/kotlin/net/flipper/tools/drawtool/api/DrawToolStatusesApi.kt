package net.flipper.tools.drawtool.api

import net.flipper.busylib.core.wrapper.CResult
import net.flipper.tools.drawtool.api.model.DrawToolDirectoryContents
import net.flipper.tools.drawtool.api.model.DrawToolStoredFile

interface DrawToolStatusesApi {
    /**
     * Every valid status of the collection, newest first
     */
    suspend fun getStatuses(): CResult<DrawToolDirectoryContents>

    /**
     * Deletes [files] from the local collection. Absent statuses are
     * skipped silently.
     */
    suspend fun deleteStatuses(files: List<DrawToolStoredFile>): CResult<Unit>

    /**
     * Path layout of the collection of this bar, for callers that address status
     * files themselves instead of hardcoding the directory structure.
     */
    suspend fun getLayout(): CResult<DrawToolStatusDirectoryLayout>
}
