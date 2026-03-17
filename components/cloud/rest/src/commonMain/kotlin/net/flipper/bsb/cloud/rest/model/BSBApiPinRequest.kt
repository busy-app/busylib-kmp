package net.flipper.bsb.cloud.rest.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BSBApiPinRequest(
    @SerialName("pin")
    val pin: String
)
