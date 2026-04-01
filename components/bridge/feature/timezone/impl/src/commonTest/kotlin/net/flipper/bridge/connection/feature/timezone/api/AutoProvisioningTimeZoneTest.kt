package net.flipper.bridge.connection.feature.timezone.api

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import net.flipper.bridge.connection.feature.timezone.api.model.TimestampInfo
import net.flipper.bridge.connection.feature.timezone.api.model.TimezoneInfo
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
    fun GIVEN_active_timezone_already_matches_target_WHEN_on_ready_THEN_does_not_set_timezone() = runTest {
        val systemAbbr = currentTimeZoneAbbreviation()
        val fake = FakeTimeZoneFeatureApi(
            timezonesResult = CResult.success(
                listOf(TimezoneInfo("London", "+00:00", systemAbbr))
            ),
            timezoneInfoFlow = flowOf(TimezoneInfo("London", "+00:00", systemAbbr)).wrap()
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
                listOf(TimezoneInfo("Current", "+00:00", systemAbbr))
            ),
            timezoneInfoFlow = flowOf(TimezoneInfo("Current", "+00:00", systemAbbr)).wrap()
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
                    TimezoneInfo("OldCity", "+00:00", "FAKE_OLD"),
                    TimezoneInfo("MatchCity", "+05:00", systemAbbr),
                    TimezoneInfo("OtherCity", "+03:00", "FAKE_OTHER")
                )
            ),
            timezoneInfoFlow = flowOf(TimezoneInfo("OldCity", "+00:00", "FAKE_OLD")).wrap()
        )
        val sut = AutoProvisioningTimeZone(fake)

        sut.onReady()

        assertEquals(1, fake.setTimezoneCalls.size)
        assertEquals("MatchCity", fake.setTimezoneCalls.first().name)
    }

    @Test
    fun GIVEN_multiple_abbr_matches_WHEN_on_ready_THEN_picks_matching_city_name() = runTest {
        val systemAbbr = currentTimeZoneAbbreviation()
        val currentCity = TimeZone.currentSystemDefault().id.substringAfterLast('/')
        val fake = FakeTimeZoneFeatureApi(
            timezonesResult = CResult.success(
                listOf(
                    TimezoneInfo("OldCity", "+00:00", "FAKE_OLD"),
                    TimezoneInfo("WrongCity", "+03:00", systemAbbr),
                    TimezoneInfo(currentCity, "+05:00", systemAbbr),
                    TimezoneInfo("AnotherWrong", "+07:00", systemAbbr)
                )
            ),
            timezoneInfoFlow = flowOf(TimezoneInfo("OldCity", "+00:00", "FAKE_OLD")).wrap()
        )
        val sut = AutoProvisioningTimeZone(fake)

        sut.onReady()

        assertEquals(1, fake.setTimezoneCalls.size)
        assertEquals(currentCity, fake.setTimezoneCalls.first().name)
    }

    @Test
    fun GIVEN_multiple_abbr_matches_no_city_match_WHEN_on_ready_THEN_picks_first() = runTest {
        val systemAbbr = currentTimeZoneAbbreviation()
        val fake = FakeTimeZoneFeatureApi(
            timezonesResult = CResult.success(
                listOf(
                    TimezoneInfo("OldCity", "+00:00", "FAKE_OLD"),
                    TimezoneInfo("Xville_A", "+03:00", systemAbbr),
                    TimezoneInfo("Xville_B", "+05:00", systemAbbr)
                )
            ),
            timezoneInfoFlow = flowOf(TimezoneInfo("OldCity", "+00:00", "FAKE_OLD")).wrap()
        )
        val sut = AutoProvisioningTimeZone(fake)

        sut.onReady()

        assertEquals(1, fake.setTimezoneCalls.size)
        assertEquals("Xville_A", fake.setTimezoneCalls.first().name)
    }

    @Test
    fun GIVEN_no_abbr_match_but_city_name_matches_WHEN_on_ready_THEN_sets_by_name() = runTest {
        val currentCity = TimeZone.currentSystemDefault().id.substringAfterLast('/')
        val fake = FakeTimeZoneFeatureApi(
            timezonesResult = CResult.success(
                listOf(
                    TimezoneInfo("OldCity", "+00:00", "FAKE_OLD"),
                    TimezoneInfo(currentCity, "+05:00", "FAKE_CITY"),
                    TimezoneInfo("OtherCity", "+03:00", "FAKE_OTHER")
                )
            ),
            timezoneInfoFlow = flowOf(TimezoneInfo("OldCity", "+00:00", "FAKE_OLD")).wrap()
        )
        val sut = AutoProvisioningTimeZone(fake)

        sut.onReady()

        assertEquals(1, fake.setTimezoneCalls.size)
        assertEquals(currentCity, fake.setTimezoneCalls.first().name)
    }

    @Test
    fun GIVEN_no_abbr_or_name_match_WHEN_on_ready_THEN_sets_closest_by_offset() = runTest {
        val fake = FakeTimeZoneFeatureApi(
            timezonesResult = CResult.success(
                listOf(
                    TimezoneInfo("Xville_1", "+00:00", "FAKE_CLOSEST"),
                    TimezoneInfo("Xville_2", "+12:00", "FAKE_FAR"),
                    TimezoneInfo("Xville_3", "-12:00", "FAKE_VERY_FAR")
                )
            ),
            timezoneInfoFlow = flowOf(TimezoneInfo("OldCity", "+09:00", "FAKE_ACTIVE")).wrap()
        )
        val sut = AutoProvisioningTimeZone(fake)

        sut.onReady()

        assertEquals(1, fake.setTimezoneCalls.size)
        val chosen = fake.setTimezoneCalls.first().name
        assertTrue(
            chosen == "Xville_1" || chosen == "Xville_2" || chosen == "Xville_3",
            "Should pick one of the available timezones"
        )
    }

    @Test
    fun GIVEN_set_timezone_fails_WHEN_on_ready_THEN_does_not_crash() = runTest {
        val systemAbbr = currentTimeZoneAbbreviation()
        val fake = FakeTimeZoneFeatureApi(
            timezonesResult = CResult.success(
                listOf(
                    TimezoneInfo("OldCity", "+00:00", "FAKE_OLD"),
                    TimezoneInfo("NewCity", "+01:00", systemAbbr)
                )
            ),
            timezoneInfoFlow = flowOf(TimezoneInfo("OldCity", "+00:00", "FAKE_OLD")).wrap(),
            setTimezoneResult = CResult.failure(RuntimeException("write error"))
        )
        val sut = AutoProvisioningTimeZone(fake)

        sut.onReady() // should not throw
    }
}

private class FakeTimeZoneFeatureApi(
    private val timezonesResult: CResult<List<TimezoneInfo>> = CResult.success(emptyList()),
    private val timezoneInfoFlow: WrappedFlow<TimezoneInfo> = flowOf(TimezoneInfo("UTC", "+00:00", "UTC")).wrap(),
    private val setTimezoneResult: CResult<Unit> = CResult.success(Unit)
) : FTimeZoneFeatureApi {
    val setTimezoneCalls = mutableListOf<TimezoneInfo>()

    override suspend fun getTimezones(): CResult<List<TimezoneInfo>> = timezonesResult

    override fun getTimeZoneInfoFlow(): WrappedFlow<TimezoneInfo> = timezoneInfoFlow

    override suspend fun setTimezone(timezoneInfo: TimezoneInfo): CResult<Unit> {
        setTimezoneCalls.add(timezoneInfo)
        return setTimezoneResult
    }

    override fun getTimestampInfoFlow(): WrappedFlow<TimestampInfo> = error("Not used")
    override suspend fun setTimestamp(timestampInfo: TimestampInfo): CResult<Unit> = error("Not used")
}
