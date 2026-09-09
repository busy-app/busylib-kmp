package net.flipper.core.busylib.data.serialization

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
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
    fun GIVEN_minute_WHEN_parse_THEN_ok() {
        assertEquals(
            expected = 1.minutes,
            actual = DurationSerializer.toDuration("1m")
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

    @Test
    fun GIVEN_duration_without_separators_WHEN_parse_THEN_ok() {
        assertEquals(
            expected = (3 * 7).days + 4.days + 6.hours + 10.minutes + 30.seconds,
            actual = DurationSerializer.toDuration("3w4d6h10m30s")
        )
    }

    @Test
    fun GIVEN_surrounding_whitespace_WHEN_parse_THEN_ok() {
        assertEquals(
            expected = 1.days + 2.hours,
            actual = DurationSerializer.toDuration("  1d 2h  ")
        )
    }

    @Test
    fun GIVEN_week_amount_above_int_range_WHEN_parse_THEN_ok() {
        assertEquals(
            expected = (500_000_000L * 7).days,
            actual = DurationSerializer.toDuration("500000000w")
        )
    }

    @Test
    fun GIVEN_blank_string_WHEN_parse_THEN_fails() {
        assertFailsWith<SerializationException> {
            DurationSerializer.toDuration("   ")
        }
    }

    @Test
    fun GIVEN_unknown_delimiter_WHEN_parse_THEN_fails() {
        assertFailsWith<SerializationException> {
            DurationSerializer.toDuration("10y")
        }
    }

    @Test
    fun GIVEN_missing_amount_WHEN_parse_THEN_fails() {
        assertFailsWith<SerializationException> {
            DurationSerializer.toDuration("xd")
        }
    }

    @Test
    fun GIVEN_zero_duration_WHEN_serialize_and_round_trip_THEN_ok() {
        val duration = Duration.ZERO
        val encoded = DurationSerializer.fromDuration(duration)
        assertEquals(
            expected = "0s",
            actual = encoded
        )
        assertEquals(
            expected = duration,
            actual = DurationSerializer.toDuration(encoded)
        )
    }

    @Test
    fun GIVEN_mixed_units_duration_WHEN_serialize_and_round_trip_THEN_ok() {
        val duration = 1.days + 1.hours + 1.seconds
        val encoded = DurationSerializer.fromDuration(duration)
        assertEquals(
            expected = "1d 1h 1s",
            actual = encoded
        )
        assertEquals(
            expected = duration,
            actual = DurationSerializer.toDuration(encoded)
        )
    }

    @Test
    fun GIVEN_whole_weeks_WHEN_serialize_THEN_days_part_is_omitted() {
        assertEquals(
            expected = "2w",
            actual = DurationSerializer.fromDuration(14.days)
        )
    }

    @Test
    fun GIVEN_minutes_overflowing_into_hours_WHEN_serialize_THEN_ok() {
        assertEquals(
            expected = "1h 30m",
            actual = DurationSerializer.fromDuration(90.minutes)
        )
    }

    @Test
    fun GIVEN_sub_second_duration_WHEN_serialize_THEN_truncated_to_seconds() {
        assertEquals(
            expected = "1s",
            actual = DurationSerializer.fromDuration(1500.milliseconds)
        )
    }

    @Test
    fun GIVEN_negative_duration_over_a_week_WHEN_serialize_and_round_trip_THEN_ok() {
        val duration = -(7.days + 1.days)
        val encoded = DurationSerializer.fromDuration(duration)
        assertEquals(
            expected = "-1w 1d",
            actual = encoded
        )
        assertEquals(
            expected = duration,
            actual = DurationSerializer.toDuration(encoded)
        )
    }

    @Test
    fun GIVEN_negative_duration_under_a_day_WHEN_serialize_and_round_trip_THEN_ok() {
        val duration = -(1.hours + 30.minutes)
        val encoded = DurationSerializer.fromDuration(duration)
        assertEquals(
            expected = "-1h 30m",
            actual = encoded
        )
        assertEquals(
            expected = duration,
            actual = DurationSerializer.toDuration(encoded)
        )
    }

    @Test
    fun GIVEN_complex_duration_WHEN_round_trip_THEN_ok() {
        val duration = 7.days + 1.days + 1.hours + 1.seconds
        val encoded = DurationSerializer.fromDuration(duration)
        val decoded = DurationSerializer.toDuration(encoded)
        assertEquals(
            expected = duration,
            actual = decoded
        )
    }

    @Test
    fun GIVEN_duration_WHEN_encoded_as_json_THEN_round_trips_as_string() {
        val duration = (3 * 7).days + 4.days + 6.hours + 10.minutes + 30.seconds
        val json = Json.encodeToString(DurationSerializer, duration)
        assertEquals(
            expected = "\"3w 4d 6h 10m 30s\"",
            actual = json
        )
        assertEquals(
            expected = duration,
            actual = Json.decodeFromString(DurationSerializer, json)
        )
    }

    @Test
    fun GIVEN_malformed_json_string_WHEN_decoded_THEN_fails() {
        assertFailsWith<SerializationException> {
            Json.decodeFromString(DurationSerializer, "\"not-a-duration\"")
        }
    }
}
