package net.flipper.bridge.connection.feature.storage.impl

import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.IOException
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.FileNotFoundException
import kotlinx.io.files.Path
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureKey
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcStorageApi
import net.flipper.bridge.connection.feature.rpc.api.model.StorageListResponse
import net.flipper.bridge.connection.feature.storage.api.FStorageFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.verbose

/**
 * Filesystem operations mapped onto the storage endpoints of the device API.
 *
 * The bar offers less than a filesystem, so some of it is emulated: there is no
 * `stat`, so metadata of a path comes from listing its parent; `mkdir` is not
 * recursive, so a tree is created level by level; and every failure is a bare
 * `400` whatever the cause, so telling "missing" from "not a directory" costs
 * an extra request — paid only on the failing path.
 */
@Suppress("TooManyFunctions")
class FStorageFeatureApiImpl(
    private val storageApi: FRpcStorageApi,
    private val pathResolver: BsbStoragePathResolver
) : FStorageFeatureApi, LogTagProvider {
    override val TAG = "FStorageFeatureApi"

    private suspend fun listEntriesOrNull(devicePath: String): List<StorageListResponse.Entry>? {
        return storageApi.listFiles(devicePath)
            .onFailure { error -> verbose { "Cannot list $devicePath: $error" } }
            .getOrNull()
            ?.list
    }

    private suspend fun findEntryOrNull(path: Path): StorageListResponse.Entry? {
        val parentPath = path.parent ?: return null
        val parentDevicePath = pathResolver.resolveOrNull(parentPath) ?: return null
        return listEntriesOrNull(parentDevicePath)
            ?.firstOrNull { entry -> entry.name == path.name }
    }

    override suspend fun metadataOrNull(path: Path): FileMetadata? {
        val devicePath = pathResolver.resolveOrNull(path) ?: return null
        if (pathResolver.isRoot(devicePath)) {
            // The root has no parent to be listed in, but being listable is
            // itself proof that it is a reachable directory.
            listEntriesOrNull(devicePath) ?: return null
            return FileMetadata(isDirectory = true, size = DIRECTORY_SIZE)
        }
        val entry = findEntryOrNull(path) ?: return null
        return when (entry.type) {
            StorageListResponse.Entry.Type.FILE -> FileMetadata(
                isRegularFile = true,
                size = entry.size ?: 0L
            )

            StorageListResponse.Entry.Type.DIR -> FileMetadata(
                isDirectory = true,
                size = DIRECTORY_SIZE
            )
        }
    }

    override suspend fun exists(path: Path): Boolean {
        return metadataOrNull(path) != null
    }

    private suspend fun throwNotListable(directory: Path): Nothing {
        val metadata = metadataOrNull(directory)
        throw when {
            metadata == null -> FileNotFoundException("No such directory on the bar: $directory")
            !metadata.isDirectory -> IOException("Not a directory on the bar: $directory")
            else -> IOException("Failed to list $directory on the bar")
        }
    }

    override suspend fun list(directory: Path): Collection<Path> {
        val devicePath = pathResolver.resolve(directory)
        val entries = listEntriesOrNull(devicePath) ?: throwNotListable(directory)
        return entries.map { entry -> Path(directory, entry.name) }
    }

    /**
     * Whether [path] still has to be created. Throws when what is already
     * there rules the creation out.
     */
    private suspend fun isCreationRequired(path: Path, mustCreate: Boolean): Boolean {
        val metadata = metadataOrNull(path) ?: return true
        if (mustCreate) {
            throw IOException("Already exists on the bar: $path")
        }
        if (!metadata.isDirectory) {
            throw IOException("Already exists as a file on the bar: $path")
        }
        return false
    }

    override suspend fun createDirectories(path: Path, mustCreate: Boolean) {
        val levelDevicePaths = pathResolver.resolveLevels(path)
        if (!isCreationRequired(path, mustCreate)) return
        // mkdir handles one level and rejects levels that are already there,
        // so every level is attempted and only the end state is checked.
        levelDevicePaths.forEach { levelDevicePath ->
            storageApi.createDirectory(levelDevicePath)
                .onFailure { error -> verbose { "Cannot mkdir $levelDevicePath: $error" } }
        }
        if (metadataOrNull(path)?.isDirectory != true) {
            throw IOException("Failed to create directories on the bar: $path")
        }
    }

    override suspend fun delete(path: Path, mustExist: Boolean) {
        val devicePath = pathResolver.resolve(path)
        storageApi.removeFile(devicePath)
            .onFailure { error ->
                if (exists(path)) {
                    throw IOException("Failed to delete $path on the bar", error)
                }
                if (mustExist) {
                    throw FileNotFoundException("No such file on the bar: $path")
                }
            }
    }

    override suspend fun atomicMove(source: Path, destination: Path) {
        val sourceDevicePath = pathResolver.resolve(source)
        val destinationDevicePath = pathResolver.resolve(destination)
        storageApi.renameFile(sourceDevicePath, destinationDevicePath)
            .onFailure { error ->
                if (!exists(source)) {
                    throw FileNotFoundException("No such file on the bar: $source")
                }
                throw IOException("Failed to move $source to $destination on the bar", error)
            }
    }

    override suspend fun source(path: Path): RawSource {
        return BsbStorageRawSource(
            devicePath = pathResolver.resolve(path),
            storageApi = storageApi
        )
    }

    /**
     * Current content of [path], or empty when there is no file to append to.
     *
     * Appending re-uploads the whole file, so an existing file that cannot be
     * read has to fail here: carrying on would silently replace its content
     * instead of extending it.
     */
    private suspend fun readExistingContentOrEmpty(path: Path, devicePath: String): ByteArray {
        if (metadataOrNull(path)?.isRegularFile != true) return ByteArray(0)
        return storageApi.readFile(devicePath)
            .getOrElse { error ->
                throw IOException("Failed to read $path for appending", error)
            }
    }

    override suspend fun sink(path: Path, append: Boolean): RawSink {
        val devicePath = pathResolver.resolve(path)
        val initialContent = when {
            append -> readExistingContentOrEmpty(path, devicePath)
            else -> ByteArray(0)
        }
        return BsbStorageRawSink(
            devicePath = devicePath,
            storageApi = storageApi,
            initialContent = initialContent
        )
    }

    override suspend fun resolve(path: Path): Path {
        val devicePath = pathResolver.resolve(path)
        if (!exists(path)) {
            throw FileNotFoundException("No such file on the bar: $path")
        }
        // Bar paths are absolute and already free of links, `.` and `..`:
        // anything else would not have passed validation.
        return Path(devicePath)
    }

    /**
     * Every storage call rides on the RPC feature, so the storage feature is
     * unavailable until that one is up.
     */
    @Inject
    @ContributesIntoMap(BusyLibGraph::class, binding<FDeviceFeatureApi.Factory>())
    @FDeviceFeatureKey(FDeviceFeature.STORAGE)
    class Factory(
        private val pathResolver: BsbStoragePathResolver
    ) : FDeviceFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FDeviceFeatureApi? {
            val fRpcFeatureApi = unsafeFeatureDeviceApi
                .get(FRpcFeatureApi::class)
                ?.await()
                ?: return null
            return FStorageFeatureApiImpl(
                storageApi = fRpcFeatureApi.fRpcStorageApi,
                pathResolver = pathResolver
            )
        }
    }

    companion object {
        /** `FileMetadata` reports a size only for regular files. */
        private const val DIRECTORY_SIZE = -1L
    }
}
