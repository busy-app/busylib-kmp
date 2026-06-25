package net.flipper.bsb.cloud.rest.model

import kotlinx.serialization.Serializable
import net.flipper.bsb.cloud.rest.serialization.FileTypeSerializer

@Serializable(with = FileTypeSerializer::class)
@Suppress("SerialNameNotProvidedRule") // Serialized via FileTypeSerializer; enum constants are not the wire format
enum class BsbFirmwareUpdateFileType {
    UPDATE_SIGNED_TGZ,
    UNKNOWN
}
