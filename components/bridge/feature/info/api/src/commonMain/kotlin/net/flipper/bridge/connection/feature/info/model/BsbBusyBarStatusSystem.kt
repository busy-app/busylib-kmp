package net.flipper.bridge.connection.feature.info.model

import kotlin.time.Duration
import kotlin.time.Instant

data class BsbBusyBarStatusSystem(
    val apiSemver: String,
    val uptime: Duration,
    val bootTime: Instant
)
