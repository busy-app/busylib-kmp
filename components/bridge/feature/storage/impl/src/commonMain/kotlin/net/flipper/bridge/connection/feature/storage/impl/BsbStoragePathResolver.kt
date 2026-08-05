package net.flipper.bridge.connection.feature.storage.impl

import dev.zacsweers.metro.Inject
import kotlinx.io.IOException
import kotlinx.io.files.Path

/**
 * Translates a [Path] into the absolute `/ext/...` string the bar storage
 * endpoints expect, rejecting what the bar could not hold in the first place.
 *
 * A path is read structurally through [Path.parent] and [Path.name] and
 * reassembled from those names. Nothing is parsed out of [Path.toString],
 * whose separator differs per platform and even per call site.
 */
@Inject
class BsbStoragePathResolver {
    private fun isValidName(name: String): Boolean {
        return name !in RELATIVE_NAMES && VALID_NAME_REGEX.matches(name)
    }

    /**
     * Names of [path] under the mount point, outermost first, or `null` when
     * the bar could not have such a path at all.
     */
    private fun relativeNamesOrNull(path: Path): List<String>? {
        val chain = generateSequence(path) { current -> current.parent }.toList()
        // A rooted path ends its parent chain at a filesystem root, whose name
        // is empty. A relative one just runs out of parents, and the bar has no
        // working directory to resolve it against.
        if (chain.last().name.isNotEmpty()) return null
        val names = chain.dropLast(1)
            .map { current -> current.name }
            .asReversed()
        if (names.firstOrNull() != ROOT_NAME) return null
        return names.drop(1).takeIf { relativeNames -> relativeNames.all(::isValidName) }
    }

    private fun devicePathOf(relativeNames: List<String>): String {
        return (listOf(ROOT_NAME) + relativeNames)
            .joinToString(separator = SEPARATOR, prefix = SEPARATOR)
    }

    private fun requireRelativeNames(path: Path): List<String> {
        return relativeNamesOrNull(path)
            ?: throw IOException("Not a valid bar storage path: $path")
    }

    /** [path] on the bar, or `null` when the bar could not have such a path. */
    fun resolveOrNull(path: Path): String? {
        return relativeNamesOrNull(path)?.let(::devicePathOf)
    }

    /**
     * [path] on the bar.
     *
     * @throws IOException when the bar could not have such a path.
     */
    fun resolve(path: Path): String {
        return devicePathOf(requireRelativeNames(path))
    }

    /**
     * Every level of [path] under the mount point, outermost first and [path]
     * itself last — the bar creates directories one level at a time. The mount
     * point is skipped: it always exists.
     *
     * @throws IOException when the bar could not have such a path.
     */
    fun resolveLevels(path: Path): List<String> {
        val relativeNames = requireRelativeNames(path)
        return relativeNames.indices.map { index ->
            devicePathOf(relativeNames.subList(0, index + 1))
        }
    }

    /** Whether [devicePath] is the mount point itself, which no listing contains. */
    fun isRoot(devicePath: String): Boolean {
        return devicePath == ROOT
    }

    companion object {
        private const val SEPARATOR = "/"

        private const val ROOT_NAME = "ext"

        /** The single writable mount point of the bar. Nothing outside it exists. */
        private const val ROOT = "$SEPARATOR$ROOT_NAME"

        /** Names the bar accepts, per the storage endpoints of the device API. */
        private val VALID_NAME_REGEX = Regex("""[a-zA-Z0-9._\-]+""")

        /**
         * The bar resolves nothing and takes a path as given, so a `..` would
         * be followed right out of [ROOT].
         */
        private val RELATIVE_NAMES = setOf(".", "..")
    }
}
