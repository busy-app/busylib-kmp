package net.flipper.core.busylib.ktx.io

import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * [FlipperFileSystem] backed by the host device filesystem, normally
 * [SystemFileSystem]. [delegate] is a parameter so tests can pass a temporary
 * or in-memory filesystem instead.
 */
class SystemFlipperFileSystem(
    private val delegate: FileSystem
) : FlipperFileSystem {
    override suspend fun exists(path: Path): Boolean {
        return delegate.exists(path)
    }

    override suspend fun delete(path: Path, mustExist: Boolean) {
        delegate.delete(path, mustExist)
    }

    override suspend fun createDirectories(path: Path, mustCreate: Boolean) {
        delegate.createDirectories(path, mustCreate)
    }

    override suspend fun atomicMove(source: Path, destination: Path) {
        delegate.atomicMove(source, destination)
    }

    override suspend fun source(path: Path): RawSource {
        return delegate.source(path)
    }

    override suspend fun sink(path: Path, append: Boolean): RawSink {
        return delegate.sink(path, append)
    }

    override suspend fun metadataOrNull(path: Path): FileMetadata? {
        return delegate.metadataOrNull(path)
    }

    override suspend fun resolve(path: Path): Path {
        return delegate.resolve(path)
    }

    override suspend fun list(directory: Path): Collection<Path> {
        return delegate.list(directory)
    }
}
