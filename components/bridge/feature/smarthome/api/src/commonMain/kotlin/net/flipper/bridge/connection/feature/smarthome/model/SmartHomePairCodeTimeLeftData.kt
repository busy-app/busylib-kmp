package net.flipper.bridge.connection.feature.smarthome.model

import kotlin.time.Duration

data class SmartHomePairCodeTimeLeftData(
    val instance: SmartHomePairCodeData,
    val timeLeft: Duration
)
