package net.flipper.bridge.connection.service.model

import net.flipper.bridge.connection.config.api.model.BUSYBar

sealed interface ExpectedState {
    data object Disconnected : ExpectedState

    data class Connected(val device: BUSYBar) : ExpectedState
}
