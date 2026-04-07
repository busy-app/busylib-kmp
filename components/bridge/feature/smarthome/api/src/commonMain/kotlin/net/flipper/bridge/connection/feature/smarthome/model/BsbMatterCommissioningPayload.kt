package net.flipper.bridge.connection.feature.smarthome.model

import kotlin.time.Instant

data class BsbMatterCommissioningPayload(
    val availableUntil: Instant,
    val qrCode: String,
    val manualCode: String
)
