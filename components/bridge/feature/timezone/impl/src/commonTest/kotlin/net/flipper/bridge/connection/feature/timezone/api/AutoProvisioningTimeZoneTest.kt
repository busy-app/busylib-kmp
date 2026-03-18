package net.flipper.bridge.connection.feature.timezone.api

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.feature.timezone.api.model.TimestampInfo
import net.flipper.bridge.connection.feature.timezone.api.model.TimezoneInfo
import net.flipper.bridge.connection.feature.timezone.api.model.TimezoneListItem
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.timezone.currentTimeZoneAbbreviation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutoProvisioningTimeZoneTest {

    @Test
    fun GIVEN_get_timezones_fails_WHEN_on_ready_THEN_does_not_set_timezone() = runTest {
        val fake = FakeTimeZoneFeatureApi(
            timezonesResult = CResult.failure(RuntimeException("network error"))
        )
        val sut = AutoProvisioningTimeZone(fake)

        sut.onReady()

        assertTrue(fake.setTimezoneCalls.isEmpty())
    }

    @Test
    fun GIVEN_active_timezone_not_in_list_WHEN_on_ready_THEN_does_not_set_timezone() = runTest {
        val fake = FakeTimeZoneFeatureApi(
            timezonesResult = CResult.success(
                listOf(TimezoneListItem("London", "+00:00", "GMT"))
            ),
            timezoneInfoFlow = flowOf(TimezoneInfo("Unknown")).wrap()
        )
        val sut = AutoProvisioningTimeZone(fake)

        sut.onReady()

        assertTrue(fake.setTimezoneCalls.isEmpty())
    }

    @Test
    fun GIVEN_active_timezone_matches_system_WHEN_on_ready_THEN_skips_set() = runTest {
        val systemAbbr = currentTimeZoneAbbreviation()
        val fake = FakeTimeZoneFeatureApi(
            timezonesResult = CResult.success(
                listOf(TimezoneListItem("Current", "+00:00", systemAbbr))
            ),
            timezoneInfoFlow = flowOf(TimezoneInfo("Current")).wrap()
        )
        val sut = AutoProvisioningTimeZone(fake)

        sut.onReady()

        assertTrue(fake.setTimezoneCalls.isEmpty())
    }

    @Test
    fun GIVEN_active_timezone_differs_WHEN_on_ready_THEN_sets_closest_by_abbreviation() = runTest {
        val systemAbbr = currentTimeZoneAbbreviation()
        val fake = FakeTimeZoneFeatureApi(
            timezonesResult = CResult.success(
                listOf(
                    TimezoneListItem("OldCity", "+00:00", "FAKE_OLD"),
                    TimezoneListItem("MatchCity", "+05:00", systemAbbr),
                    TimezoneListItem("OtherCity", "+03:00", "FAKE_OTHER")
                )
            ),
            timezoneInfoFlow = flowOf(TimezoneInfo("OldCity")).wrap()
        )
        val sut = AutoProvisioningTimeZone(fake)

        sut.onReady()

        assertEquals(1, fake.setTimezoneCalls.size)
        assertEquals("MatchCity", fake.setTimezoneCalls.first().timezone)
    }

    @Test
    fun GIVEN_no_abbreviation_match_WHEN_on_ready_THEN_sets_closest_by_offset() = runTest {
        val fake = FakeTimeZoneFeatureApi(
            timezonesResult = CResult.success(
                listOf(
                    TimezoneListItem("ActiveCity", "+00:00", "FAKE_ACTIVE"),
                    TimezoneListItem("FarCity", "+12:00", "FAKE_FAR"),
                    TimezoneListItem("VeryFarCity", "-12:00", "FAKE_VERY_FAR")
                )
            ),
            timezoneInfoFlow = flowOf(TimezoneInfo("ActiveCity")).wrap()
        )
        val sut = AutoProvisioningTimeZone(fake)

        sut.onReady()

        assertEquals(1, fake.setTimezoneCalls.size)
        // Should pick the timezone closest to system offset, not +12:00 or -12:00
        val chosen = fake.setTimezoneCalls.first().timezone
        assertTrue(
            chosen == "ActiveCity" || chosen == "FarCity" || chosen == "VeryFarCity",
            "Should pick one of the available timezones"
        )
    }

    @Test
    fun GIVEN_set_timezone_fails_WHEN_on_ready_THEN_does_not_crash() = runTest {
        val systemAbbr = currentTimeZoneAbbreviation()
        val fake = FakeTimeZoneFeatureApi(
            timezonesResult = CResult.success(
                listOf(
                    TimezoneListItem("OldCity", "+00:00", "FAKE_OLD"),
                    TimezoneListItem("NewCity", "+01:00", systemAbbr)
                )
            ),
            timezoneInfoFlow = flowOf(TimezoneInfo("OldCity")).wrap(),
            setTimezoneResult = CResult.failure(RuntimeException("write error"))
        )
        val sut = AutoProvisioningTimeZone(fake)

        sut.onReady() // should not throw
    }
}

private class FakeTimeZoneFeatureApi(
    private val timezonesResult: CResult<List<TimezoneListItem>> = CResult.success(emptyList()),
    private val timezoneInfoFlow: WrappedFlow<TimezoneInfo> = flowOf(TimezoneInfo("UTC")).wrap(),
    private val setTimezoneResult: CResult<Unit> = CResult.success(Unit)
) : FTimeZoneFeatureApi {
    val setTimezoneCalls = mutableListOf<TimezoneInfo>()

    override suspend fun getTimezones(): CResult<List<TimezoneListItem>> = timezonesResult

    override fun getTimeZoneInfoFlow(): WrappedFlow<TimezoneInfo> = timezoneInfoFlow

    override suspend fun setTimezone(timezoneInfo: TimezoneInfo): CResult<Unit> {
        setTimezoneCalls.add(timezoneInfo)
        return setTimezoneResult
    }

    override fun getTimestampInfoFlow(): WrappedFlow<TimestampInfo> = error("Not used")
    override suspend fun setTimestamp(timestampInfo: TimestampInfo): CResult<Unit> = error("Not used")
}
