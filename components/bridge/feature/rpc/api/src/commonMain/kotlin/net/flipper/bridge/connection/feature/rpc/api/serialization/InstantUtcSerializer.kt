package net.flipper.bridge.connection.feature.rpc.api.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

/**
 * This serializer will transform [Instant] into UTC millisecond
 */
object InstantUtcSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "flipper.instant.utc",
        kind = PrimitiveKind.LONG
    )

    override fun deserialize(decoder: Decoder): Instant {
        val long = decoder.decodeLong()
        return Instant.fromEpochMilliseconds(long)
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        val long = value.toEpochMilliseconds()
        encoder.encodeLong(long)
    }
}
