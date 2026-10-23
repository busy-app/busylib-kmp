package net.flipper.bridge.connection.feature.rpc.api.exception

import net.flipper.bridge.connection.feature.rpc.api.model.BsbRpcError

/**
 * `POST /api/name` refused the name, so the bar keeps its previous one.
 *
 * @param reason the bar's own message: one of the `NAME_*` entries of [BsbRpcError]
 * when the firmware documents the case, free text otherwise.
 */
class DeviceNameRejectedException(val reason: String) : Throwable(reason)
