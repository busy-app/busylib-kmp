package net.flipper.bridge.connection.service.model

import net.flipper.bridge.connection.config.api.model.BUSYBar

sealed interface ConnectionAction {
    data object Skip : ConnectionAction

    data object Disconnect : ConnectionAction

    data class Connect(val device: BUSYBar) : ConnectionAction
}
