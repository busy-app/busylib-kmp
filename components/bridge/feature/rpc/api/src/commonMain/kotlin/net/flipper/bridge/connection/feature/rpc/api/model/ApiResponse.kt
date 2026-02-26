package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.bridge.connection.feature.rpc.api.serialization.ApiResponseSerializer

@Serializable(ApiResponseSerializer::class)
sealed interface ApiResponse

@Serializable
data class SuccessResponse(
    @SerialName("result")
    val result: String
) : ApiResponse

// hello world
@Serializable
data class ErrorResponse(
    @SerialName("error")
    val error: String
) : ApiResponse
