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

    private val WiFiSecurity.Supported.Password.serialName: String
        get() = when (this) {
            WiFiSecurity.Supported.Password.WEP -> "WEP"
            WiFiSecurity.Supported.Password.WPA -> "WPA"
            WiFiSecurity.Supported.Password.WPA2 -> "WPA2"
            WiFiSecurity.Supported.Password.WPA_WPA2 -> "WPA_WPA2"
            WiFiSecurity.Supported.Password.WPA3 -> "WPA3"
            WiFiSecurity.Supported.Password.WPA2_WPA3 -> "WPA2_WPA3"
        }

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
