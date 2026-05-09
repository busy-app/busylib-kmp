package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.core.busylib.data.serialization.InstantUtcSerializer
import kotlin.time.Instant

@Serializable
data class SmartHomePairingPayload(
    @SerialName("available_until")
    @Serializable(InstantUtcSerializer::class)
    val availableUntil: Instant,
    @SerialName("qr_code")
    val qrCode: kotlin.String,
    @SerialName("manual_code")
    val manualCode: kotlin.String
)
