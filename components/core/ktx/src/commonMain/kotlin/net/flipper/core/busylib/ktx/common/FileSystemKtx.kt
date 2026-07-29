package net.flipper.core.busylib.ktx.common

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.readByteArray
import net.flipper.core.busylib.ktx.io.FlipperFileSystem

/**
 * Atomically replaces the content of [path]: the content goes into a sibling
 * temporary file first and is then moved over the destination, so a reader never
 * sees a half-written file.
 *
 * @throws IllegalArgumentException when [path] has no parent to hold that
 * temporary file.
 */
suspend fun FlipperFileSystem.replaceFileContent(path: Path, content: ByteArray) {
    val parentPath = requireNotNull(path.parent) {
        "Cannot atomically replace a path without a parent: $path"
    }
    createDirectories(parentPath, mustCreate = false)
    val temporaryPath = Path(parentPath, "${path.name}.tmp")
    writeFileBytes(temporaryPath, content)
    atomicMove(temporaryPath, path)
}

/** Reads [path] whole into memory. */
suspend fun FlipperFileSystem.readFileBytes(path: Path): ByteArray {
    return source(path).buffered().use { source -> source.readByteArray() }
}

/**
 * Overwrites [path] with [content]. Not atomic — a reader can catch the file
 * mid-write; use [replaceFileContent] when that matters.
 */
suspend fun FlipperFileSystem.writeFileBytes(path: Path, content: ByteArray) {
    sink(path).buffered().use { sink -> sink.write(content) }
}

/** Streams [sourcePath] into [destinationPath] without loading it into memory. */
suspend fun FlipperFileSystem.copyFile(sourcePath: Path, destinationPath: Path) {
    source(sourcePath).buffered().use { source ->
        sink(destinationPath).buffered().use { sink ->
            source.transferTo(sink)
        }
    }
}

/** Deletes a file, or a directory with everything in it. Absent [path] is a no-op. */
suspend fun FlipperFileSystem.deleteRecursively(path: Path) {
    val metadata = metadataOrNull(path) ?: return
    if (metadata.isDirectory) {
        list(path).forEach { childPath -> deleteRecursively(childPath) }
    }
    delete(path, mustExist = false)
}

/**
 * Children of [directory], or nothing when it is missing or is not a directory
 * — for the callers to which "no directory" and "an empty one" are the same.
 */
suspend fun FlipperFileSystem.listOrEmpty(directory: Path): Collection<Path> {
    val metadata = metadataOrNull(directory) ?: return emptyList()
    if (!metadata.isDirectory) return emptyList()
    return list(directory)
}

/** Every regular file under [directory], recursively. */
suspend fun FlipperFileSystem.walkRegularFiles(directory: Path): List<Path> {
    return listOrEmpty(directory).flatMap { childPath ->
        val metadata = metadataOrNull(childPath)
        when {
            metadata == null -> emptyList()
            metadata.isDirectory -> walkRegularFiles(childPath)
            metadata.isRegularFile -> listOf(childPath)
            else -> emptyList()
        }
    }
}
