package net.flipper.bridge.connection.screens.dashboard.drawtool

/**
 * Which filesystem a Draw tool collection action runs against.
 *
 * The whole point of the sample is that both targets are plain
 * [net.flipper.core.busylib.ktx.io.FlipperFileSystem] implementations, so the
 * same generator and the same reader work against either of them.
 */
enum class DrawToolStorageTarget(val title: String) {
    /** The collection on this device, resolved by the library from the bar serial. */
    CLIENT("Client"),

    /** The collection on the connected BUSY Bar, reached over the storage feature. */
    BUSY_BAR("BUSY Bar")
}
