package net.flipper.bridge.connection.screens.search

import net.flipper.bridge.connection.config.api.model.FDeviceBaseModel

data class ConnectionSearchItem(
    val address: String,
    val deviceModel: FDeviceBaseModel,
    val isAdded: Boolean
)
