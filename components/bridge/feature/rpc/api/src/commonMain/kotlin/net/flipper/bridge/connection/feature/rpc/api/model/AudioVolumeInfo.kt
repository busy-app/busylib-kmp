package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AudioVolumeInfo(
    @SerialName("volume")
    val volume: Double
)
