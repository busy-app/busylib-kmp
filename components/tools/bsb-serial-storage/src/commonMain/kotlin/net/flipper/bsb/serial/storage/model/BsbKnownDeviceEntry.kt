package net.flipper.bsb.serial.storage.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BsbKnownDeviceEntry(
    @SerialName("unique_id")
    val uniqueId: String,
    @SerialName("serial")
    val serial: String
)
