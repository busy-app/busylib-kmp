package net.flipper.tools.drawtool.storage.api

import kotlinx.io.files.Path

/**
 * Platform-specific Draw tool root on the client (mobile/macOS/desktop), for
 * example `<app data>/busylib/drawer`. It holds one collection per known bar,
 * at `<root>/<device serial>`.
 */
interface DrawToolStoragePathProvider {
    fun getPath(): Result<Path>
}
