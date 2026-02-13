package net.flipper.bridge.connection.screens.search

import net.flipper.bridge.connection.config.api.model.BUSYBar

data class ConnectionSearchItem(
    val address: String,
    val deviceModel: BUSYBar,
    val isAdded: Boolean
)
