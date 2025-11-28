package net.flipper.bridge.connection.feature.rpc.impl.exposed.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceNameResponse(
    @SerialName("name")
    val name: String
)