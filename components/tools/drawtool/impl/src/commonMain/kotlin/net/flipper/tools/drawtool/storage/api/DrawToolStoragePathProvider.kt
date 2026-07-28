package net.flipper.tools.drawtool.storage.api

import kotlinx.io.files.Path

/**
 * Platform-owned location of the Draw tool drawer root
 * (`<app data>/busylib/drawer`). The client application is not involved:
 * the library resolves its own storage on every platform. The drawer root
 * holds the collection of each known bar in `<root>/<device serial>` plus
 * the private client files of the Draw tool.
 */
interface DrawToolStoragePathProvider {
    /** Fails when the platform cannot resolve its application data directory. */
    fun getDrawerRootPath(): Result<Path>
}
