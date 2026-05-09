package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateStatusInstallDownload(
    @SerialName("speed_bytes_per_sec")
    val speedBytesPerSec: kotlin.Int,
    @SerialName("received_bytes")
    val receivedBytes: kotlin.Int,
    @SerialName("total_bytes")
    val totalBytes: kotlin.Int
)
