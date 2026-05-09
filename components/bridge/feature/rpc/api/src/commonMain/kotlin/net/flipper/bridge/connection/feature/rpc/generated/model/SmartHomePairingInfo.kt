package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SmartHomePairingInfo(
    @SerialName("fabric_count")
    val fabricCount: kotlin.Int,
    @SerialName("latest_pairing_status")
    val latestPairingStatus: SmartHomePairingInfoLatestPairingStatus
)
