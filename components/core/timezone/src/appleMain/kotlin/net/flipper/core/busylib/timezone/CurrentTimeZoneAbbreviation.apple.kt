package net.flipper.core.busylib.timezone

import platform.Foundation.NSTimeZone

actual fun currentTimeZoneAbbreviation(): String {
    return NSTimeZone.localTimeZone.abbreviation ?: ""
}
