package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusySnapshot(
    @SerialName("snapshot")
    val snapshot: BusySnapshotSnapshot,
    @SerialName("snapshot_timestamp_ms")
    val snapshotTimestampMs: kotlin.Int
)
