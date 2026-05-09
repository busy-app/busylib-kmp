package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StorageStatus(
    @SerialName("used_bytes")
    val usedBytes: kotlin.Int? = null,
    @SerialName("free_bytes")
    val freeBytes: kotlin.Int? = null,
    @SerialName("total_bytes")
    val totalBytes: kotlin.Int? = null
)
