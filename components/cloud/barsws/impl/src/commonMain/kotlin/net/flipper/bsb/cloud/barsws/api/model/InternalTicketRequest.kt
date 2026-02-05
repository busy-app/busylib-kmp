package net.flipper.bsb.cloud.barsws.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InternalTicketRequest(
    @SerialName("token")
    val token: String
)
