package net.flipper.bridge.connection.config.impl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.bridge.connection.config.api.model.BUSYBar

@Serializable
data class BleConfigSettings(
    @SerialName("current_selected_device_id")
    val currentSelectedDeviceId: String? = null,
    @SerialName("devices")
    val devices: List<BUSYBar> = emptyList()
)
