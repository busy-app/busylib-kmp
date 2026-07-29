package net.flipper.tools.drawtool.api.model

/** Role of a file inside a status directory, per the layout of the spec. */
enum class DrawToolFileType {
    /** A render frame `frameNNN.png`; a static status has a single one. */
    FRAME,

    /** The `preview.png` shown in the status list. */
    PREVIEW,

    /** A scene media source in `assets/`, named by its content hash. */
    ASSET,

    /** The packed `status.anim` the bar plays for animated statuses. */
    ANIMATION,

    /**
     * Not recognized by this client version. Kept so that statuses written by
     * newer clients survive a read/save round trip untouched.
     */
    OTHER
}
