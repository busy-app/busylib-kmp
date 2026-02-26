package net.flipper.bridge.connection.feature.rpc.api.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.flipper.bridge.connection.feature.rpc.api.model.ApiResponse
import net.flipper.bridge.connection.feature.rpc.api.model.ErrorResponse
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse

object ApiResponseSerializer : KSerializer<ApiResponse> {
    @Serializable
    private class ApiResponseSurrogate(
        @SerialName("error")
        val error: String? = null,
        @SerialName("result")
        val result: String? = null
    )

    override val descriptor: SerialDescriptor = SerialDescriptor(
        serialName = "net.flipper.bsb.ApiResponseSurrogate",
        original = ApiResponseSurrogate.serializer().descriptor
    )

    override fun serialize(encoder: Encoder, value: ApiResponse) {
        val surrogate = when (value) {
            is ErrorResponse -> ApiResponseSurrogate(error = value.error)
            is SuccessResponse -> ApiResponseSurrogate(result = value.result)
        }
        encoder.encodeSerializableValue(ApiResponseSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): ApiResponse {
        val surrogate = decoder.decodeSerializableValue(ApiResponseSurrogate.serializer())
        return when {
            surrogate.error != null -> ErrorResponse(error = surrogate.error)
            surrogate.result != null -> SuccessResponse(result = surrogate.result)
            else -> error("ApiResponseSurrogate must have either error or result")
        }
    }
}
