package net.flipper.bridge.connection.feature.settings.mapper

import net.flipper.bridge.connection.feature.rpc.api.exception.DeviceNameRejectedException
import net.flipper.bridge.connection.feature.rpc.api.model.BsbRpcError
import net.flipper.bridge.connection.feature.settings.model.DeviceNameError

internal fun Throwable.toDeviceNameError(): Throwable {
    if (this !is DeviceNameRejectedException) return this
    return when (reason) {
        BsbRpcError.NAME_DISALLOWED_CHARACTER.error -> DeviceNameError.DisallowedCharacter()
        BsbRpcError.NAME_EMPTY.error -> DeviceNameError.Empty()
        BsbRpcError.NAME_TOO_LONG.error -> DeviceNameError.TooLong()
        else -> this
    }
}
