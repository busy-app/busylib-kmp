package net.flipper.bsb.cloud.rest.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BsbFirmwareUpdateChannel(
    /**
     * Don't use here [BsbFirmwareChannelId] as we don't know what channel names can be
     */
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("description")
    val description: String,
    @SerialName("versions")
    val versions: List<BsbFirmwareUpdateVersion>
)
