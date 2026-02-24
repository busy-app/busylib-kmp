package net.flipper.bsb.cloud.rest.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusyFirmwareDirectory(
    @SerialName("channels")
    val channels: List<BsbFirmwareUpdateChannel>
) {
}

