package net.flipper.bridge.connection.service.impl

import net.flipper.bridge.connection.config.api.model.BUSYBar

sealed interface ExpectedState {
    data object Disconnected : ExpectedState

    data class Connected(val device: BUSYBar) : ExpectedState
}
