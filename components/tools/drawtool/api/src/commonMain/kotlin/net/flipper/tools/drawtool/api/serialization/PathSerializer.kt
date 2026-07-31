package net.flipper.tools.drawtool.api.serialization

import kotlinx.io.files.Path
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object PathSerializer : KSerializer<Path> {
    override val descriptor = PrimitiveSerialDescriptor(
        serialName = "FractionWholeSerializer",
        kind = PrimitiveKind.INT
    )

    override fun serialize(encoder: Encoder, value: Path) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Path {
        return Path(decoder.decodeString())
    }
}
