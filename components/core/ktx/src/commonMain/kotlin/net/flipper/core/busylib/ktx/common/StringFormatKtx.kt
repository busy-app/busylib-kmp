package net.flipper.core.busylib.ktx.common

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.readByteArray
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.serializer
import net.flipper.core.busylib.ktx.io.FlipperFileSystem

/**
 * Reads [path] whole and decodes it.
 *
 * The file is held in memory while decoding, so this is for small metadata
 * documents like `project.json`, not for status content.
 */
suspend fun <T> StringFormat.decodeFromFile(
    kSerializer: KSerializer<T>,
    fileSystem: FlipperFileSystem,
    path: Path
): Result<T> {
    return runSuspendCatching {
        decodeFromString(
            deserializer = kSerializer,
            string = fileSystem.source(path)
                .buffered()
                .use { source -> source.readByteArray() }
                .decodeToString()
        )
    }
}

/** [decodeFromFile] with the serializer taken from [T]. */
suspend inline fun <reified T> StringFormat.decodeFromFile(
    fileSystem: FlipperFileSystem,
    path: Path
): Result<T> = decodeFromFile(
    kSerializer = serializer<T>(),
    fileSystem = fileSystem,
    path = path
)

/**
 * Encodes [value] into [path] via [replaceFileContent], so a concurrent reader
 * sees either the old document or the new one, never half of either.
 */
suspend fun <T> StringFormat.encodeToFile(
    kSerializer: KSerializer<T>,
    fileSystem: FlipperFileSystem,
    path: Path,
    value: T
): Result<Unit> {
    return runSuspendCatching {
        fileSystem.replaceFileContent(
            path = path,
            content = encodeToString(kSerializer, value).encodeToByteArray()
        )
    }
}

/** [encodeToFile] with the serializer taken from [T]. */
suspend inline fun <reified T> StringFormat.encodeToFile(
    fileSystem: FlipperFileSystem,
    path: Path,
    value: T
): Result<Unit> = encodeToFile(
    kSerializer = serializer<T>(),
    fileSystem = fileSystem,
    path = path,
    value = value
)
