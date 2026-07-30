package net.flipper.bridge.connection.screens.dashboard.drawtool

/**
 * Which filesystem a collection action runs against. Both are plain
 * [net.flipper.core.busylib.ktx.io.FlipperFileSystem]s, so one generator and one
 * reader serve either.
 */
enum class DrawToolStorageTarget(val title: String) {
    /** The collection on this device, resolved by the library from the bar serial. */
    CLIENT("Client"),

    /** The collection on the connected BUSY Bar, reached over the storage feature. */
    BUSY_BAR("BUSY Bar")
}
