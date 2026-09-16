package net.flipper.core.busylib.ktx.io

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * [FlipperFileSystem] backed by the host device filesystem, normally
 * [SystemFileSystem]. [delegate] is a parameter so tests can pass a temporary
 * or in-memory filesystem instead. [FileSystem] is blocking, so every call is
 * moved onto [dispatcher].
 */
class SystemFlipperFileSystem(
    private val delegate: FileSystem,
    private val dispatcher: CoroutineDispatcher
) : FlipperFileSystem {
    override suspend fun exists(path: Path): Boolean {
        return withContext(dispatcher) {
            delegate.exists(path)
        }
    }

    override suspend fun delete(path: Path, mustExist: Boolean) {
        withContext(dispatcher) {
            delegate.delete(path, mustExist)
        }
    }

    override suspend fun createDirectories(path: Path, mustCreate: Boolean) {
        withContext(dispatcher) {
            delegate.createDirectories(path, mustCreate)
        }
    }

    override suspend fun atomicMove(source: Path, destination: Path) {
        withContext(dispatcher) {
            delegate.atomicMove(source, destination)
        }
    }

    override suspend fun source(path: Path): RawSource {
        return withContext(dispatcher) {
            delegate.source(path)
        }
    }

    override suspend fun sink(path: Path, append: Boolean): RawSink {
        return withContext(dispatcher) {
            delegate.sink(path, append)
        }
    }

    override suspend fun metadataOrNull(path: Path): FileMetadata? {
        return withContext(dispatcher) {
            delegate.metadataOrNull(path)
        }
    }

    override suspend fun resolve(path: Path): Path {
        return withContext(dispatcher) {
            delegate.resolve(path)
        }
    }

    override suspend fun list(directory: Path): Collection<Path> {
        return withContext(dispatcher) {
            delegate.list(directory)
        }
    }
}
