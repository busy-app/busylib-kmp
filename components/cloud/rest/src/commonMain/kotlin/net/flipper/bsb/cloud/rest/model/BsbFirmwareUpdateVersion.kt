package net.flipper.bsb.cloud.rest.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BsbFirmwareUpdateVersion(
    @SerialName("version")
    val version: String,
    @SerialName("changelog")
    val changelog: String,
    @SerialName("timestamp")
    val timestamp: Long,
    @SerialName("files")
    val files: List<BsbFirmwareUpdateFileEntry>
) {
}