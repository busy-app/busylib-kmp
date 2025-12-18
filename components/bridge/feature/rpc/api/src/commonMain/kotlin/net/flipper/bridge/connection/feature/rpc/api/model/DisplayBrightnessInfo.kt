package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DisplayBrightnessInfo(
    @SerialName("front")
    val front: String,
    @SerialName("back")
    val back: String
)
