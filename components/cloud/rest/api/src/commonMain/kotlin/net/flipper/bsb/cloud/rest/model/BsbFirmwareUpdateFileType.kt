package net.flipper.bsb.cloud.rest.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.bsb.cloud.rest.serialization.FileTypeSerializer

@Serializable(with = FileTypeSerializer::class)
enum class BsbFirmwareUpdateFileType {

    @SerialName("update_signed_tgz")
    UPDATE_SIGNED_TGZ,

    @SerialName("UNKNOWN")
    UNKNOWN
}
