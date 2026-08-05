package net.flipper.bridge.connection.feature.storage.impl

import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcStorageApi
import net.flipper.bridge.connection.feature.rpc.api.model.StorageListResponse
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse

/**
 * In-memory stand-in for the storage endpoints of a bar.
 *
 * It reproduces the traits the code under test has to work around, rather than
 * only recording calls: `mkdir` creates a single level and rejects a level that
 * already exists, a write needs its parent directory to be there, listing works
 * on directories only and reading on files only, and every refusal is a plain
 * error — the bar answers `400` to all of them alike.
 */
internal class FakeFRpcStorageApi : FRpcStorageApi {
    private val directories = mutableSetOf(ROOT)
    private val files = mutableMapOf<String, ByteArray>()

    /** Every request that reached the bar, as `"<verb> <path>"`, in order. */
    val requests = mutableListOf<String>()

    /** Requests, in the form used by [requests], the bar must refuse. */
    val refusedRequests = mutableSetOf<String>()

    /**
     * Listings to answer verbatim, for response shapes the bar may send but
     * this fake would never produce on its own.
     */
    val listingOverrides = mutableMapOf<String, StorageListResponse>()

    /** Runs before every request, to let concurrent callers interleave. */
    var onRequest: suspend () -> Unit = {}

    private fun parentOf(path: String): String {
        return path.substringBeforeLast(SEPARATOR).ifEmpty { SEPARATOR.toString() }
    }

    private fun nameOf(path: String): String {
        return path.substringAfterLast(SEPARATOR)
    }

    private fun isDirectory(path: String): Boolean = path in directories

    private fun isFile(path: String): Boolean = path in files

    private fun exists(path: String): Boolean = isDirectory(path) || isFile(path)

    private fun childrenOf(path: String): List<String> {
        return (directories + files.keys).filter { candidate ->
            candidate != path && parentOf(candidate) == path
        }
    }

    private fun <T> refuse(reason: String): Result<T> {
        return Result.failure(IllegalStateException(reason))
    }

    private fun accept(): Result<SuccessResponse> {
        return Result.success(SuccessResponse(result = "OK"))
    }

    private suspend fun <T> request(
        verb: String,
        path: String,
        answer: () -> Result<T>
    ): Result<T> {
        onRequest()
        val request = "$verb $path"
        requests += request
        if (request in refusedRequests) return refuse("Refused: $request")
        return answer()
    }

    override suspend fun writeFile(
        path: String,
        content: ByteArray
    ): Result<SuccessResponse> = request(verb = "write", path = path) {
        when {
            isDirectory(path) -> refuse("Is a directory: $path")
            !isDirectory(parentOf(path)) -> refuse("No parent directory of $path")
            else -> {
                files[path] = content.copyOf()
                accept()
            }
        }
    }

    override suspend fun readFile(path: String): Result<ByteArray> = request(
        verb = "read",
        path = path
    ) {
        val content = files[path] ?: return@request refuse("No such file: $path")
        Result.success(content.copyOf())
    }

    override suspend fun listFiles(path: String): Result<StorageListResponse> = request(
        verb = "list",
        path = path
    ) {
        val override = listingOverrides[path]
        if (override != null) return@request Result.success(override)
        if (!isDirectory(path)) return@request refuse("Not a directory: $path")
        val entries = childrenOf(path).map { child ->
            val content = files[child]
            StorageListResponse.Entry(
                type = when (content) {
                    null -> StorageListResponse.Entry.Type.DIR
                    else -> StorageListResponse.Entry.Type.FILE
                },
                name = nameOf(child),
                size = content?.size?.toLong()
            )
        }
        Result.success(StorageListResponse(list = entries))
    }

    override suspend fun createDirectory(path: String): Result<SuccessResponse> = request(
        verb = "mkdir",
        path = path
    ) {
        when {
            exists(path) -> refuse("Already exists: $path")
            !isDirectory(parentOf(path)) -> refuse("No parent directory of $path")
            else -> {
                directories += path
                accept()
            }
        }
    }

    override suspend fun removeFile(path: String): Result<SuccessResponse> = request(
        verb = "remove",
        path = path
    ) {
        when {
            isFile(path) -> {
                files -= path
                accept()
            }

            !isDirectory(path) -> refuse("No such file: $path")
            childrenOf(path).isNotEmpty() -> refuse("Directory not empty: $path")
            else -> {
                directories -= path
                accept()
            }
        }
    }

    private fun move(path: String, newPath: String) {
        val descendantPrefix = "$path$SEPARATOR"
        val movedDirectories = directories.filter { candidate ->
            candidate == path || candidate.startsWith(descendantPrefix)
        }
        val movedFiles = files.keys.filter { candidate ->
            candidate == path || candidate.startsWith(descendantPrefix)
        }
        movedDirectories.forEach { candidate ->
            directories -= candidate
            directories += newPath + candidate.removePrefix(path)
        }
        movedFiles.forEach { candidate ->
            val content = files.remove(candidate) ?: return@forEach
            files[newPath + candidate.removePrefix(path)] = content
        }
    }

    override suspend fun renameFile(
        path: String,
        newPath: String
    ): Result<SuccessResponse> = request(verb = "rename", path = path) {
        when {
            !exists(path) -> refuse("No such file: $path")
            !isDirectory(parentOf(newPath)) -> refuse("No parent directory of $newPath")
            else -> {
                move(path, newPath)
                accept()
            }
        }
    }

    /** Puts a directory on the bar together with every level leading to it. */
    fun putDirectory(path: String) {
        var current = path
        while (current.startsWith(ROOT)) {
            directories += current
            if (current == ROOT) break
            current = parentOf(current)
        }
    }

    /** Puts a file on the bar together with the directories leading to it. */
    fun putFile(path: String, content: ByteArray) {
        putDirectory(parentOf(path))
        files[path] = content.copyOf()
    }

    fun fileContentOrNull(path: String): ByteArray? = files[path]?.copyOf()

    fun hasFile(path: String): Boolean = isFile(path)

    fun hasDirectory(path: String): Boolean = isDirectory(path)

    fun requestsOf(verb: String): List<String> {
        return requests.filter { request -> request.startsWith("$verb ") }
    }

    companion object {
        const val ROOT = "/ext"

        private const val SEPARATOR = '/'
    }
}
