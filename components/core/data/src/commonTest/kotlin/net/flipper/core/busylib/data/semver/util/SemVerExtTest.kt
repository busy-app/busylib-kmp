@file:Suppress("FunctionNaming")

package net.flipper.core.busylib.data.semver.util

import net.flipper.core.busylib.data.semver.model.SemVer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SemVerExtTest {

    @Test
    fun GIVEN_plain_versions_WHEN_parse_THEN_missing_parts_are_zero() {
        assertEquals(
            expected = SemVer(
                majorVersion = 1,
                minorVersion = 2,
                patchVersion = 0,
                additionalVersion = 0
            ),
            actual = "1.2".toSemVerOrNull()
        )
        assertEquals(
            expected = SemVer(
                majorVersion = 1,
                minorVersion = 2,
                patchVersion = 3,
                additionalVersion = 0
            ),
            actual = "1.2.3".toSemVerOrNull()
        )
        assertEquals(
            expected = SemVer(
                majorVersion = 1,
                minorVersion = 2,
                patchVersion = 3,
                additionalVersion = 4
            ),
            actual = "1.2.3.4".toSemVerOrNull()
        )
    }

    @Test
    fun GIVEN_pre_release_or_build_suffix_WHEN_parse_THEN_suffix_is_dropped() {
        val expected = SemVer(
            majorVersion = 1,
            minorVersion = 4,
            patchVersion = 0,
            additionalVersion = 0
        )
        assertEquals(expected, "1.4.0-rc".toSemVerOrNull())
        assertEquals(expected, "1.4.0-rc1".toSemVerOrNull())
        assertEquals(expected, "1.4.0+build5".toSemVerOrNull())
        assertEquals(expected, "1.4.0-rc1+build5".toSemVerOrNull())
        assertEquals(expected, "1.4.0+build-5".toSemVerOrNull())
        assertEquals(
            expected = SemVer(
                majorVersion = 1,
                minorVersion = 2,
                patchVersion = 0,
                additionalVersion = 0
            ),
            actual = "1.2-rc".toSemVerOrNull()
        )
    }

    @Test
    fun GIVEN_malformed_version_WHEN_parse_THEN_null() {
        assertNull("".toSemVerOrNull())
        assertNull("1".toSemVerOrNull())
        assertNull("1.2.3.4.5".toSemVerOrNull())
        assertNull("a.b".toSemVerOrNull())
        assertNull("1..2".toSemVerOrNull())
        assertNull("1.2.".toSemVerOrNull())
        assertNull("v1.2.3".toSemVerOrNull())
        assertNull("1.x.3".toSemVerOrNull())
        assertNull(" 1.2".toSemVerOrNull())
        assertNull("-rc".toSemVerOrNull())
        assertNull("99999999999.1".toSemVerOrNull())
    }

    @Test
    fun GIVEN_version_with_and_without_trailing_zero_parts_WHEN_parse_THEN_same_value() {
        val short = "6.3".toSemVerOrNull()

        assertEquals(short, "6.3.0".toSemVerOrNull())
        assertEquals(short, "6.3.0.0".toSemVerOrNull())
    }
}
