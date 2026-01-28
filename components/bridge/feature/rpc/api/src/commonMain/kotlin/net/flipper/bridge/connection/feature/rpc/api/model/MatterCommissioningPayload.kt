package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.bridge.connection.feature.rpc.api.serialization.InstantUtcSerializer
import kotlin.time.Instant

data class MatterCommissioningPayload(
    @SerialName("available_until")
    @Serializable(InstantUtcSerializer::class)
    val availableUntil: Instant,
    @SerialName("qr_code")
    val qrCode: String,
    @SerialName("manual_code")
    val manualCode: String
)
