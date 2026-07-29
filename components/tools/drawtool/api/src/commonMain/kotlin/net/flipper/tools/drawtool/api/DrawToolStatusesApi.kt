package net.flipper.tools.drawtool.api

import kotlinx.collections.immutable.ImmutableList
import kotlinx.io.files.Path
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.tools.drawtool.api.model.DrawToolStatus

interface DrawToolStatusesApi {
    /**
     * Names of every entry of the collection directory, unfiltered — temporary
     * and trash directories included. Use [getStatuses] for actual statuses;
     * this exists so a generated status id can be checked against everything
     * already on disk.
     *
     * @param uniqueId is uniqueId of BusyBar inside FPersistentStorage
     */
    suspend fun getStatusIds(uniqueId: String): CResult<ImmutableList<String>>

    /**
     * Every valid status of the collection, newest first. A status with a
     * missing or unreadable `project.json` is skipped.
     *
     * @param uniqueId is uniqueId of BusyBar inside FPersistentStorage
     */
    suspend fun getStatuses(uniqueId: String): CResult<ImmutableList<DrawToolStatus>>

    /**
     * The status with [statusId], or `null` when its directory is absent or
     * incomplete.
     *
     * @param uniqueId is uniqueId of BusyBar inside FPersistentStorage
     */
    suspend fun getStatus(
        uniqueId: String,
        statusId: String
    ): CResult<DrawToolStatus?>

    /**
     * Deletes [statusIds] from the local collection. Absent statuses are
     * skipped silently.
     *
     * @param uniqueId is uniqueId of BusyBar inside FPersistentStorage
     */
    suspend fun deleteStatuses(
        uniqueId: String,
        statusIds: ImmutableList<String>
    ): CResult<Unit>

    /**
     * Collection directory of the bar on the client device.
     *
     * Example: `/<android_app_path>/busylib/drawer/<BUSY bar serial number>/`
     *
     * @param uniqueId is uniqueId of BusyBar inside FPersistentStorage
     */
    suspend fun getCollectionPath(uniqueId: String): CResult<Path>

    /**
     * Path layout of the collection of this bar, for callers that address status
     * files themselves instead of hardcoding the directory structure.
     *
     * @param uniqueId is uniqueId of BusyBar inside FPersistentStorage
     */
    suspend fun getLayout(uniqueId: String): CResult<DrawToolStatusDirectoryLayout>
}
