package net.flipper.busylib

import kotlin.time.Duration
import kotlin.time.DurationUnit

fun Duration.toNSTimeInterval(): Double {
    require(this.isFinite())
    return this.toDouble(DurationUnit.SECONDS)
}
