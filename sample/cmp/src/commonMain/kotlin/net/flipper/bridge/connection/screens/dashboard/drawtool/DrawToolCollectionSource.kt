package net.flipper.bridge.connection.screens.dashboard.drawtool

import kotlinx.io.files.Path
import net.flipper.core.busylib.ktx.io.FlipperFileSystem
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.api.DrawToolStatusesApi

/**
 * One Draw tool collection resolved for a [DrawToolStorageTarget]: everything
 * an action needs, in a form that does not reveal which target it came from.
 *
 * [fileSystem] and [layout] are what writing a status needs, [statusesApi] is
 * what reading it back needs, and [collectionPath] is the directory both work
 * in — created before a write and reported in the log.
 */
class DrawToolCollectionSource(
    val collectionPath: Path,
    val fileSystem: FlipperFileSystem,
    val layout: DrawToolStatusDirectoryLayout,
    val statusesApi: DrawToolStatusesApi
)
