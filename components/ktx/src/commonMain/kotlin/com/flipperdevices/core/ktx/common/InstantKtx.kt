package com.flipperdevices.core.ktx.common

import kotlin.time.Instant

val Instant.Companion.ZERO: Instant
    get() = Instant.fromEpochMilliseconds(0L)
