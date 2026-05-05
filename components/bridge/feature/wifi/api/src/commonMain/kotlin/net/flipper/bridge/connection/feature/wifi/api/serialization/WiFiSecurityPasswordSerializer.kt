package net.flipper.bridge.connection.feature.wifi.api.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity

internal object WiFiSecurityPasswordSerializer : KSerializer<WiFiSecurity.Supported.Password> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "WiFiSecurity.Supported.Password",
        kind = PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: WiFiSecurity.Supported.Password) {
        encoder.encodeString(value.serialName)
    }

    override fun deserialize(decoder: Decoder): WiFiSecurity.Supported.Password {
        val serialName = decoder.decodeString()
        return WiFiSecurity.Supported.Password
            .entries
            .firstOrNull { entry -> entry.serialName == serialName }
            ?: error("Could not find $serialName password type")
    }
}
