package net.flipper.tools.drawtool.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DrawToolDirectoryContents(
    @SerialName("files")
    val files: List<DrawToolStoredFile>
)
