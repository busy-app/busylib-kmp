package net.flipper.core.busylib.data.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.flipper.core.busylib.data.Fraction

object FractionIntWholeSerializer : KSerializer<Fraction> {
    override val descriptor = PrimitiveSerialDescriptor(
        serialName = "FractionWholeSerializer",
        kind = PrimitiveKind.INT
    )

    override fun serialize(encoder: Encoder, value: Fraction) {
        encoder.encodeInt(value.toWholePercent().toInt())
    }

    override fun deserialize(decoder: Decoder): Fraction {
        return Fraction.fromWholePercent(decoder.decodeInt())
    }
}
