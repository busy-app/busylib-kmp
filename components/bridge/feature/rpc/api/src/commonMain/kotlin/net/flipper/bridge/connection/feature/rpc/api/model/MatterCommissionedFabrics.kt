package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.bridge.connection.feature.rpc.api.serialization.InstantUtcSerializer
import kotlin.time.Instant

@Serializable
class MatterCommissionedFabrics(
    @SerialName("fabric_count")
    val fabricCount: Int,
    @SerialName("latest_commissioning_status")
    val latestCommissioningStatus: CommissioningStatus
) {
    @Serializable
    data class CommissioningStatus(
        @Serializable(InstantUtcSerializer::class)
        @SerialName("timestamp")
        val timestamp: Instant,
        @SerialName("value")
        val value: CommissioningStatusType
    )

    @Serializable
    enum class CommissioningStatusType {
        @SerialName("never_started")
        NEVER_STARTED,

        @SerialName("started")
        STARTED,

        @SerialName("completed_successfully")
        COMPLETED_SUCCESSFULLY,

        @SerialName("failed")
        FAILED
    }
}
