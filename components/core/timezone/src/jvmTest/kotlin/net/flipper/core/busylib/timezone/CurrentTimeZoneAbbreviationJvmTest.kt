package net.flipper.core.busylib.timezone

import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class CurrentTimeZoneAbbreviationJvmTest {

    @Test
    fun GIVEN_utc_timezone_WHEN_get_abbreviation_THEN_returns_UTC() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            assertEquals("UTC", currentTimeZoneAbbreviation())
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun GIVEN_gmt_timezone_WHEN_get_abbreviation_THEN_returns_GMT() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
            assertEquals("GMT", currentTimeZoneAbbreviation())
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
