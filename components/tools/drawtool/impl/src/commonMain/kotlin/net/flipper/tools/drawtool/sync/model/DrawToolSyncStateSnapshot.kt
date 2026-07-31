package net.flipper.tools.drawtool.sync.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The persistent sync memory. [syncedBySerial] names are recorded only after a
 * confirmed transfer, so "remembered but absent" always means "deleted on that
 * side". [tombstones] are kept forever, so a deleted status can never be
 * brought back by a bar that still holds it.
 */
@Serializable
data class DrawToolSyncStateSnapshot(
    @SerialName("synced_by_serial")
    val syncedBySerial: Map<String, Set<DrawToolStatusName>> = emptyMap(),
    @SerialName("tombstones")
    val tombstones: Set<DrawToolStatusName> = emptySet(),
)
