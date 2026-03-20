package net.flipper.core.busylib.timezone

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CurrentTimeZoneAbbreviationTest {

    @Test
    fun GIVEN_system_timezone_WHEN_get_abbreviation_THEN_returns_non_empty_string() {
        val abbr = currentTimeZoneAbbreviation()
        assertTrue(abbr.isNotEmpty(), "Timezone abbreviation should not be empty")
    }

    @Test
    fun GIVEN_system_timezone_WHEN_get_abbreviation_twice_THEN_returns_same_value() {
        val first = currentTimeZoneAbbreviation()
        val second = currentTimeZoneAbbreviation()
        assertEquals(first, second)
    }
}
