package net.flipper.bridge.connection.feature.rpc.api.exception

import net.flipper.bridge.connection.feature.rpc.api.model.BsbRpcError

/**
 * `POST /api/display/draw` was refused because something already on the display
 * was requested with a higher priority.
 *
 * The bar answers `409 Conflict` with [BsbRpcError.NOT_DRAWN_LOW_PRIORITY]. Nothing
 * was drawn and nothing was changed, so retrying is only useful once whatever holds
 * the display releases it.
 */
class DrawLowPriorityException : Throwable(BsbRpcError.NOT_DRAWN_LOW_PRIORITY.error)
