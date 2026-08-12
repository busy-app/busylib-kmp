package net.flipper.bridge.connection.feature.wifi.api.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity
import net.flipper.bridge.connection.feature.wifi.api.serialization.model.WiFiSecuritySurrogate
import net.flipper.bridge.connection.feature.wifi.api.serialization.model.toSurrogate
import net.flipper.bridge.connection.feature.wifi.api.serialization.model.toWiFiSecurity

internal object WiFiSecuritySerializer : KSerializer<WiFiSecurity> {
    override val descriptor: SerialDescriptor = SerialDescriptor(
        serialName = "net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity",
        original = WiFiSecuritySurrogate.serializer().descriptor
    )

    override fun serialize(encoder: Encoder, value: WiFiSecurity) {
        encoder.encodeSerializableValue(WiFiSecuritySurrogate.serializer(), value.toSurrogate())
    }

    override fun deserialize(decoder: Decoder): WiFiSecurity {
        return decoder.decodeSerializableValue(WiFiSecuritySurrogate.serializer()).toWiFiSecurity()
    }
}
