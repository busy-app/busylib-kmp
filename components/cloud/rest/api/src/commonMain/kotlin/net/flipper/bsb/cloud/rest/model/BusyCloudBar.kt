package net.flipper.bsb.cloud.rest.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusyCloudBar(
    @SerialName("id")
    val id: String,
    @SerialName("label")
    val label: String? = null
)
