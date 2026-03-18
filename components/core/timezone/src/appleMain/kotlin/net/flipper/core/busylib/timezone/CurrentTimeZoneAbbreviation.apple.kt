package net.flipper.core.busylib.timezone

import platform.Foundation.NSTimeZone
import platform.Foundation.abbreviation
import platform.Foundation.localTimeZone

actual fun currentTimeZoneAbbreviation(): String {
    return NSTimeZone.localTimeZone.abbreviation ?: ""
}
