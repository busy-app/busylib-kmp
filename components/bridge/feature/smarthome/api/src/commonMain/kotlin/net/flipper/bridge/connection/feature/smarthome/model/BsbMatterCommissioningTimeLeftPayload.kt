package net.flipper.bridge.connection.feature.smarthome.model

import kotlin.time.Duration

data class BsbMatterCommissioningTimeLeftPayload(
    val instance: BsbMatterCommissioningPayload,
    val timeLeft: Duration
)
