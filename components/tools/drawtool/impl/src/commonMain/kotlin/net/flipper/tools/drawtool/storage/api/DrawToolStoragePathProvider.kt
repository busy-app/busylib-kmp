package net.flipper.tools.drawtool.storage.api

import kotlinx.io.files.Path

/**
 * Platform-specific location of the Draw tool root folder
 * Example: `<app data>/busylib/drawer`
 * This folder holds the collections of each known bar in `<root>/<device serial>`
 */
interface DrawToolStoragePathProvider {
    fun getPath(): Result<Path>
}
