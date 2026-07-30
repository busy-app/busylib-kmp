package net.flipper.tools.drawtool.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Role of a file inside a status directory, per the layout of the spec. */
@Serializable
enum class DrawToolFileType {
    /** The `preview.png` shown in the status list. */
    @SerialName("PREVIEW")
    PREVIEW,

    /**
     * The status file named YYYY-mm-dd_HH-mm-ss.png
     */
    @SerialName("STATUS")
    STATUS,
}
