package net.flipper.bridge.connection.service.impl

import net.flipper.bridge.connection.config.api.model.FDeviceBaseModel

sealed interface ExpectedState {
    data object Disconnected : ExpectedState

    data class Connected(val device: FDeviceBaseModel) : ExpectedState
}
