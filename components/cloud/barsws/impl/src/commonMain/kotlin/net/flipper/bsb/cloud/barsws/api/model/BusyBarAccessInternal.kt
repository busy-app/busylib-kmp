package net.flipper.bsb.cloud.barsws.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class BusyBarAccessInternal(
    @SerialName("granted")
    val granted: Boolean,
    @SerialName("grace_until")
    val graceUntil: Instant? = null
)
