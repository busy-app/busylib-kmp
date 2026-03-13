package net.flipper.bridge.connection.feature.rpc.api.serialization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class DurationSerializerTest {

    @Test
    fun GIVEN_second_WHEN_parse_THEN_ok() {
        assertEquals(
            expected = 1.seconds,
            actual = DurationSerializer.toDuration("1s")
        )
    }

    @Test
    fun GIVEN_hour_WHEN_parse_THEN_ok() {
        assertEquals(
            expected = 1.hours,
            actual = DurationSerializer.toDuration("1h")
        )
    }

    @Test
    fun GIVEN_day_WHEN_parse_THEN_ok() {
        assertEquals(
            expected = 1.days,
            actual = DurationSerializer.toDuration("1d")
        )
    }

    @Test
    fun GIVEN_week_WHEN_parse_THEN_ok() {
        assertEquals(
            expected = 7.days,
            actual = DurationSerializer.toDuration("1w")
        )
    }

    @Test
    fun GIVEN_custom_duration_WHEN_parse_THEN_ok() {
        assertEquals(
            expected = 7.days + 1.hours,
            actual = DurationSerializer.toDuration("1w 1h")
        )

        assertEquals(
            expected = 1.days + 1.hours + 1.seconds,
            actual = DurationSerializer.toDuration("1d 1h 1s")
        )

        assertEquals(
            expected = 7.days + 1.days + 1.hours + 1.seconds,
            actual = DurationSerializer.toDuration("1w 1d 1h 1s")
        )
    }
}
