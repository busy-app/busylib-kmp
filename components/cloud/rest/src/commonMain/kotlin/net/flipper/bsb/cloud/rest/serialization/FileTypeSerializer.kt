package net.flipper.bsb.cloud.rest.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.flipper.bsb.cloud.rest.model.BsbFirmwareUpdateFileType

internal object FileTypeSerializer : KSerializer<BsbFirmwareUpdateFileType> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "net.flipper.bsb.cloud.firmware.update.FileType",
        kind = PrimitiveKind.STRING
    )

    override fun deserialize(decoder: Decoder): BsbFirmwareUpdateFileType {
        return when (decoder.decodeString()) {
            "update_tgz" -> BsbFirmwareUpdateFileType.UPDATE_TGZ
            else -> BsbFirmwareUpdateFileType.UNKNOWN
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: BsbFirmwareUpdateFileType
    ) {
        val serialized = when (value) {
            BsbFirmwareUpdateFileType.UPDATE_TGZ -> "update_tgz"
            BsbFirmwareUpdateFileType.UNKNOWN -> "UNKNOWN"
        }
        encoder.encodeString(serialized)
    }
}
