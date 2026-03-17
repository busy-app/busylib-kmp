package net.flipper.bsb.cloud.rest.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.bsb.cloud.rest.serialization.FileTypeSerializer

@Serializable(with = FileTypeSerializer::class)
enum class BsbFirmwareUpdateFileType {
    @SerialName("UPDATE_TGZ")
    UPDATE_TGZ,

    @SerialName("UNKNOWN")
    UNKNOWN
}
