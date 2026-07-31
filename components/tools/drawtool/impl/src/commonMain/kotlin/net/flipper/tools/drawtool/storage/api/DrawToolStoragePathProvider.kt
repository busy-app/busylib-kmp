package net.flipper.tools.drawtool.storage.api

import kotlinx.io.files.Path

/**
 * Platform-specific Draw tool collection on the client (mobile/macOS/desktop),
 * for example `<app data>/busylib/draw_tool` — one flat directory of statuses,
 * not keyed by bar.
 */
interface DrawToolStoragePathProvider {
    fun getPath(): Result<Path>
}
