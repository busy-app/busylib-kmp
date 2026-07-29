package net.flipper.tools.drawtool.collection.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.core.busylib.data.serialization.InstantUtcSerializer
import kotlin.time.Instant

/**
 * `project.json` — the only metadata file of a status directory and its commit
 * point: it is written last, and a directory without a parseable one does not
 * exist for readers.
 *
 * Currently reduced to the schema version and the last edit time; the remaining
 * spec fields (frames, assets, derived, scene) come later. `updated_at` is Unix
 * seconds, as the sync protocol requires.
 */
@Serializable
data class DrawToolProjectFile(
    @SerialName("schema_version")
    val schemaVersion: Int,
    @SerialName("updated_at")
    @Serializable(InstantUtcSerializer::class)
    val updatedAt: Instant
) {
    companion object {
        /** Version this client writes. Bumped when the stored shape changes. */
        const val CURRENT_SCHEMA_VERSION = 1

        /** A `project.json` stamped with [CURRENT_SCHEMA_VERSION]. */
        fun of(updatedAt: Instant): DrawToolProjectFile {
            return DrawToolProjectFile(
                schemaVersion = CURRENT_SCHEMA_VERSION,
                updatedAt = updatedAt
            )
        }
    }
}
