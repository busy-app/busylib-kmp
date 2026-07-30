package net.flipper.tools.drawtool.api.model

import kotlinx.io.files.Path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.tools.drawtool.api.serialization.PathSerializer

/**
 * A reference to one file of a Draw tool status. The library never loads the
 * content into memory: it stays on disk and is streamed when copied.
 *
 * [path] is absolute. For statuses returned by the library it points inside the
 * local collection; when saving a status it points at the caller's source file,
 * which the library copies into the collection.
 */
@Serializable
data class DrawToolStoredFile(
    @SerialName("type")
    val type: DrawToolFileType,
    @SerialName("path")
    @Serializable(PathSerializer::class)
    val path: Path,
)
