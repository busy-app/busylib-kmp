package net.flipper.bsb.cloud.barsws.api.model

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import net.flipper.core.busylib.ktx.common.tryCast

object WebSocketEventInternalSerializer : KSerializer<WebSocketEventInternal> {

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = SerialDescriptor(
        serialName = "net.flipper.bsb.cloud.barsws.api.model.WebSocketEventInternal",
        original = WebSocketFrameSurrogate.serializer().descriptor
    )

    private val eventSerializersByType = listOf(
        WebSocketEventInternal.LinkDevice.serializer(),
        WebSocketEventInternal.UnlinkDevice.serializer(),
        WebSocketEventInternal.NameUpdated.serializer(),
        WebSocketEventInternal.Protobuf.serializer()
    ).associateBy { serializer -> serializer.descriptor.serialName }

    private fun selectDeserializer(
        surrogate: WebSocketFrameSurrogate
    ): DeserializationStrategy<WebSocketEventInternal> {
        return when {
            surrogate.type != null -> {
                eventSerializersByType[surrogate.type]
                    ?: throw SerializationException("Unknown websocket event type: ${surrogate.type}")
            }

            surrogate.error != null -> WebSocketEventInternal.SubscriptionError.serializer()

            else -> throw SerializationException("Websocket frame is neither a typed event nor an error response")
        }
    }

    override fun deserialize(decoder: Decoder): WebSocketEventInternal {
        val jsonDecoder = decoder.tryCast<JsonDecoder>()
            ?: throw SerializationException("WebSocketEventInternal can only be decoded from JSON")
        val element = jsonDecoder.decodeJsonElement()
        val surrogate = jsonDecoder.json.decodeFromJsonElement(
            deserializer = WebSocketFrameSurrogate.serializer(),
            element = element
        )
        return jsonDecoder.json.decodeFromJsonElement(
            deserializer = selectDeserializer(surrogate),
            element = element
        )
    }

    override fun serialize(encoder: Encoder, value: WebSocketEventInternal) {
        throw SerializationException("WebSocketEventInternal is a receive-only wire type and cannot be serialized")
    }
}
