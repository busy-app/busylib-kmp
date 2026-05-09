package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayAudio(
    @SerialName("application_name")
    val applicationName: kotlin.String,
    @SerialName("path")
    val path: kotlin.String,
    @SerialName("stock_path")
    val stockPath: kotlin.String
)
