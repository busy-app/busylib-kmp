package net.flipper.core.busylib.ktx.io

import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.FileNotFoundException
import kotlinx.io.files.Path

/**
 * Basic filesystem operations, mirroring `kotlinx.io.files.FileSystem`.
 *
 * It exists only because `kotlinx.io.files.FileSystem` is a `sealed interface`
 * and cannot be implemented outside of `kotlinx.io`, which rules out both
 * remote filesystems and test doubles. Signatures and contracts are kept
 * identical, so once kotlinx-io drops `sealed` this interface can be dropped
 * mechanically. [Path], [FileMetadata], [RawSource] and [RawSink] are reused
 * from kotlinx-io as is.
 *
 * An implementation may talk to a remote filesystem, so a single call can take
 * as long as a network request.
 *
 * Implementations are not thread safe unless they document otherwise.
 */
interface FlipperFileSystem {
    /**
     * Whether [path] points to an existing filesystem entity.
     *
     * @throws kotlinx.io.IOException when the check itself failed.
     */
    suspend fun exists(path: Path): Boolean

    /**
     * Deletes the file or directory [path] points to. Not recursive: a
     * non-empty directory is not deleted.
     *
     * @param mustExist when `true`, a missing [path] is an error.
     * @throws FileNotFoundException when [path] does not exist and [mustExist] is `true`.
     * @throws kotlinx.io.IOException if deletion failed.
     */
    suspend fun delete(path: Path, mustExist: Boolean = true)

    /**
     * Creates the directory tree [path], creating only the missing levels.
     * Not atomic: a failure may leave some of them behind.
     *
     * @param mustCreate when `true`, an already existing [path] is an error.
     * @throws kotlinx.io.IOException when [path] exists and [mustCreate] is `true`.
     * @throws kotlinx.io.IOException when creating one of the directories fails.
     */
    suspend fun createDirectories(path: Path, mustCreate: Boolean = false)

    /**
     * Atomically renames [source] to [destination], overwriting an existing
     * [destination].
     *
     * @throws FileNotFoundException when [source] does not exist.
     * @throws kotlinx.io.IOException when the move failed.
     * @throws UnsupportedOperationException when the filesystem cannot move atomically.
     */
    suspend fun atomicMove(source: Path, destination: Path)

    /**
     * Opens [path] for reading. Failures may surface only once the source is
     * actually read from.
     *
     * @throws FileNotFoundException when the file does not exist.
     * @throws kotlinx.io.IOException when the file cannot be opened for reading.
     */
    suspend fun source(path: Path): RawSource

    /**
     * Opens [path] for writing, creating it when missing. Failures may surface
     * only once the sink is written to.
     *
     * @param append when `true`, data is appended instead of overwriting.
     * @throws kotlinx.io.IOException when the file cannot be opened for writing.
     */
    suspend fun sink(path: Path, append: Boolean = false): RawSink

    /**
     * Metadata of [path], or `null` when there is no such entity or metadata
     * cannot be fetched.
     */
    suspend fun metadataOrNull(path: Path): FileMetadata?

    /**
     * Absolute path to the same entity as [path], with symbolic links, `.` and
     * `..` resolved.
     *
     * @throws FileNotFoundException when [path] does not exist.
     */
    suspend fun resolve(path: Path): Path

    /**
     * Immediate children of [directory], in no particular order. Children of
     * an absolute [directory] are absolute too.
     *
     * @throws FileNotFoundException when [directory] does not exist.
     * @throws kotlinx.io.IOException when [directory] is not a directory, or listing it failed.
     */
    suspend fun list(directory: Path): Collection<Path>
}
