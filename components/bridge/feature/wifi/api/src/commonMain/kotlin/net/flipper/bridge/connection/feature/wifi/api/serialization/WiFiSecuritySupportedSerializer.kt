package net.flipper.bridge.connection.feature.wifi.api.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity
import net.flipper.bridge.connection.feature.wifi.api.serialization.model.WiFiSecuritySurrogate
import net.flipper.bridge.connection.feature.wifi.api.serialization.model.toSupportedWiFiSecurity
import net.flipper.bridge.connection.feature.wifi.api.serialization.model.toSurrogate

internal object WiFiSecuritySupportedSerializer : KSerializer<WiFiSecurity.Supported> {
    override val descriptor: SerialDescriptor = SerialDescriptor(
        serialName = "net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity.Supported",
        original = WiFiSecuritySurrogate.serializer().descriptor
    )

    override fun serialize(encoder: Encoder, value: WiFiSecurity.Supported) {
        encoder.encodeSerializableValue(WiFiSecuritySurrogate.serializer(), value.toSurrogate())
    }

    override fun deserialize(decoder: Decoder): WiFiSecurity.Supported {
        return decoder
            .decodeSerializableValue(WiFiSecuritySurrogate.serializer())
            .toSupportedWiFiSecurity()
    }
}
