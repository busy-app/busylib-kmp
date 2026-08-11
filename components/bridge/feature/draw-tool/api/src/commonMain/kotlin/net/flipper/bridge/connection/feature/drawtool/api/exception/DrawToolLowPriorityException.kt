package net.flipper.bridge.connection.feature.drawtool.api.exception

/**
 * The bar refused to show the drawing because something with a higher priority
 * currently owns the display — an active work session, for instance.
 *
 * Carried by the failed
 * [CResult][net.flipper.busylib.core.wrapper.CResult] of
 * [showPreview][net.flipper.bridge.connection.feature.drawtool.api.FDrawToolFeatureApi.showPreview]
 * and
 * [showFile][net.flipper.bridge.connection.feature.drawtool.api.FDrawToolFeatureApi.showFile].
 *
 * Nothing was drawn and nothing was changed on the bar. This is an expected outcome
 * rather than a malfunction, so it is worth reporting to the user differently from a
 * transport or protocol error.
 */
class DrawToolLowPriorityException : Throwable(
    "Not drawn: the BUSY Bar display is owned by something with a higher priority"
)
