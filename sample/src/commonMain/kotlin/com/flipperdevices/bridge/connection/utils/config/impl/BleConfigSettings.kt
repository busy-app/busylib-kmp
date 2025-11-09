package com.flipperdevices.bridge.connection.utils.config.impl

import com.flipperdevices.bridge.connection.config.api.model.FDeviceBaseModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BleConfigSettings(
    @SerialName("current_selected_device_id")
    val currentSelectedDeviceId: String? = null,
    @SerialName("devices")
    val devices: List<FDeviceBaseModel> = emptyList()
)
