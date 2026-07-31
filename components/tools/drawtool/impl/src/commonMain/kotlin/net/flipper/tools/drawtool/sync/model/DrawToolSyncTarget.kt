package net.flipper.tools.drawtool.sync.model

import net.flipper.core.busylib.ktx.io.FlipperFileSystem
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout

/** The bar one sync pass runs against. */
data class DrawToolSyncTarget(
    val serialNumber: String,
    val barFileSystem: FlipperFileSystem,
    val barLayout: DrawToolStatusDirectoryLayout,
)
