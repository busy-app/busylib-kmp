package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Error(
    @SerialName("error")
    val error: kotlin.String,
    @SerialName("code")
    val code: kotlin.Int? = null
)
