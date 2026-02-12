package net.flipper.bridge.connection.screens.search

import net.flipper.bridge.connection.config.api.model.FDeviceCombined

data class ConnectionSearchItem(
    val address: String,
    val deviceModel: FDeviceCombined,
    val isAdded: Boolean
)
