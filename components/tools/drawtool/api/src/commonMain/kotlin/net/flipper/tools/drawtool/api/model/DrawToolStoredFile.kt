package net.flipper.tools.drawtool.api.model

import kotlinx.io.files.Path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.tools.drawtool.api.serialization.PathSerializer

/**
 * One file of a Draw tool collection, by role. [path] is absolute and stays on
 * disk: the content is streamed, not held.
 */
@Serializable
sealed interface DrawToolStoredFile {
    @SerialName("path")
    val path: Path

    /** The `temp.png` working file. */
    @Serializable
    @SerialName("PREVIEW")
    data class Preview(
        @SerialName("path")
        @Serializable(PathSerializer::class)
        override val path: Path,
    ) : DrawToolStoredFile

    /** A committed status, `YYYY-mm-dd_HH_mm_ss.png`. */
    @Serializable
    @SerialName("STATUS")
    data class Status(
        @SerialName("path")
        @Serializable(PathSerializer::class)
        override val path: Path,
    ) : DrawToolStoredFile
}
