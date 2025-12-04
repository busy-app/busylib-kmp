package net.flipper.bridge.connection.feature.rpc.api.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.flipper.bridge.connection.feature.rpc.api.model.BsbBrightness

object BsbBrightnessSerializer : KSerializer<BsbBrightness> {

    override val descriptor = PrimitiveSerialDescriptor(
        serialName = "net.flipper.BsbBrightness",
        kind = PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: BsbBrightness) {
        val str = when (value) {
            is BsbBrightness.Auto -> "auto"
            is BsbBrightness.Percentage -> "${value.value}"
        }
        encoder.encodeString(str)
    }

    private fun getAutoBrightnessOrNull(string: String): BsbBrightness.Auto? {
        return BsbBrightness.Auto
            .takeIf { string.equals("auto", true) }
    }

    private fun getPercentageBrightnessOrNull(string: String): BsbBrightness.Percentage? {
        return string
            .toIntOrNull()
            ?.takeIf { int -> int > 0 }
            ?.takeIf { int -> int in 0..100 }
            ?.let(BsbBrightness::Percentage)
    }

    override fun deserialize(decoder: Decoder): BsbBrightness {
        val str = decoder.decodeString()
        return getAutoBrightnessOrNull(str)
            ?: getPercentageBrightnessOrNull(str)
            ?: throw SerializationException("Brightness must be 'auto' or integer 0..100")
    }
}
