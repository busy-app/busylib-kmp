package net.flipper.core.busylib.data.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

/**
 * This serializer will transform [Instant] into UTC Unix seconds,
 * unlike [InstantUtcSerializer] which uses milliseconds
 */
object InstantEpochSecondsSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "flipper.instant.epoch.seconds",
        kind = PrimitiveKind.LONG
    )

    override fun deserialize(decoder: Decoder): Instant {
        val seconds = decoder.decodeLong()
        return Instant.fromEpochSeconds(seconds)
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.epochSeconds)
    }
}
