package net.flipper.core.busylib.timezone

import java.util.Date
import java.util.TimeZone

actual fun currentTimeZoneAbbreviation(): String {
    val tz = TimeZone.getDefault()
    return tz.getDisplayName(tz.inDaylightTime(Date()), TimeZone.SHORT)
}
