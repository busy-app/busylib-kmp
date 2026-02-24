package net.flipper.bsb.cloud.rest.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BsbFirmwareUpdateFileEntry(
    @SerialName("url")
    val url: String,
    @SerialName("target")
    val target: BsbFirmwareUpdateTarget,
    @SerialName("type")
    val type: BsbFirmwareUpdateFileType,
    @SerialName("sha256")
    val sha256: String
)
