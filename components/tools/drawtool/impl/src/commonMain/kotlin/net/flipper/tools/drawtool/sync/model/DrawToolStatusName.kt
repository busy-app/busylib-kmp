package net.flipper.tools.drawtool.sync.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * The file name of a status (`YYYY-mm-dd_HH_mm_ss.png`) — the identity
 * statuses are compared, remembered and tombstoned by.
 */
@Serializable
@JvmInline
value class DrawToolStatusName(
    @SerialName("value")
    val value: String,
)
