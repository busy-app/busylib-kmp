package net.flipper.bsb.cloud.rest.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class BusyCloudTicketResponse(
    @SerialName("token")
    val token: String
)
