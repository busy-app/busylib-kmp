package net.flipper.bridge.connection.feature.rpc.api.exposed

import net.flipper.bridge.connection.feature.rpc.api.model.StorageListResponse
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse

/**
 * Raw access to the internal storage of the bar.
 *
 * Every path is absolute and rooted at `/ext`, the only writable mount point.
 *
 * The endpoints behind this API transfer whole files: there is no way to read
 * or write a byte range, and no way to append. Callers that need filesystem
 * semantics on top of this should use the storage feature instead of composing
 * these calls by hand.
 */
interface FRpcStorageApi {
    suspend fun writeFile(
        path: String,
        content: ByteArray
    ): Result<SuccessResponse>

    /** Downloads the whole file at [path]. */
    suspend fun readFile(path: String): Result<ByteArray>

    /**
     * Lists the immediate children of the directory at [path].
     *
     * Fails when [path] does not exist or is not a directory: the bar answers
     * `400` to both, so the two cases are indistinguishable from the response.
     */
    suspend fun listFiles(path: String): Result<StorageListResponse>

    suspend fun createDirectory(path: String): Result<SuccessResponse>

    suspend fun removeFile(path: String): Result<SuccessResponse>

    /** Moves [path] to [newPath], overwriting [newPath] if it exists. */
    suspend fun renameFile(
        path: String,
        newPath: String
    ): Result<SuccessResponse>
}
