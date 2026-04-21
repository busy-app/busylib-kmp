package net.flipper.bsb.cloud.rest.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class BusyCloudBar(
    @SerialName("id")
    val id: Uuid,
    @SerialName("hardware_id")
    val hardwareId: String,
    @SerialName("label")
    val label: String? = null
)
