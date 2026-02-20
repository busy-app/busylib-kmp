package net.flipper.bridge.connection.feature.rpc.impl.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InternalBrightnessInfo(
    @SerialName("value")
    val value: String
)
