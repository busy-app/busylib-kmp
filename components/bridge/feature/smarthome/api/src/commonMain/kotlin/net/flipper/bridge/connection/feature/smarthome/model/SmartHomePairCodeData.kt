package net.flipper.bridge.connection.feature.smarthome.model

import kotlin.time.Instant

data class SmartHomePairCodeData(
    val code: String,
    val url: String,
    val expiresAfter: Instant
)
