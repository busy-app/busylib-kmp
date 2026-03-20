package net.flipper.core.busylib.timezone

import platform.Foundation.NSTimeZone
import platform.Foundation.abbreviation
import platform.Foundation.localTimeZone

actual fun currentTimeZoneAbbreviation(): String {
    val tz = NSTimeZone.localTimeZone
    return tz.abbreviation ?: tz.name
}
