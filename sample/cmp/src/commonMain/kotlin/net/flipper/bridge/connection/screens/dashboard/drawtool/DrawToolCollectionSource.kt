package net.flipper.bridge.connection.screens.dashboard.drawtool

import kotlinx.io.files.Path
import net.flipper.core.busylib.ktx.io.FlipperFileSystem
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.api.DrawToolStatusesApi

/**
 * A [DrawToolStorageTarget] resolved into what an action needs, with the target
 * itself no longer visible: [fileSystem] and [layout] write a status,
 * [statusesApi] reads it back, [collectionPath] is the directory of both.
 */
class DrawToolCollectionSource(
    val collectionPath: Path,
    val fileSystem: FlipperFileSystem,
    val layout: DrawToolStatusDirectoryLayout,
    val statusesApi: DrawToolStatusesApi
)
