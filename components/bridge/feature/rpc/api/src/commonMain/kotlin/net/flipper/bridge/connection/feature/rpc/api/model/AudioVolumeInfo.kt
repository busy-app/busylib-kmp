package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.bridge.connection.feature.rpc.api.serialization.FractionIntWholeSerializer

@Serializable
data class AudioVolumeInfo(
    @SerialName("volume")
    @Serializable(FractionIntWholeSerializer::class)
    val volume: Fraction
)
