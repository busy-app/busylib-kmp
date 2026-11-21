@file:Suppress("FunctionNaming")

package net.flipper.core.busylib.data.semver.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SemVerTest {

    @Test
    fun GIVEN_higher_part_WHEN_compare_THEN_wins_over_lower_parts() {
        assertTrue(
            SemVer(
                majorVersion = 2,
                minorVersion = 0,
                patchVersion = 0,
                additionalVersion = 0
            ) > SemVer(
                majorVersion = 1,
                minorVersion = 9,
                patchVersion = 9,
                additionalVersion = 9
            )
        )
        assertTrue(
            SemVer(
                majorVersion = 6,
                minorVersion = 3,
                patchVersion = 0,
                additionalVersion = 0
            ) < SemVer(
                majorVersion = 6,
                minorVersion = 3,
                patchVersion = 9,
                additionalVersion = 0
            )
        )
        assertTrue(
            SemVer(
                majorVersion = 1,
                minorVersion = 2,
                patchVersion = 3,
                additionalVersion = 0
            ) < SemVer(
                majorVersion = 1,
                minorVersion = 2,
                patchVersion = 3,
                additionalVersion = 1
            )
        )
    }

    @Test
    fun GIVEN_multi_digit_parts_WHEN_compare_THEN_compared_numerically() {
        assertTrue(
            SemVer(
                majorVersion = 1,
                minorVersion = 10,
                patchVersion = 0,
                additionalVersion = 0
            ) > SemVer(
                majorVersion = 1,
                minorVersion = 9,
                patchVersion = 0,
                additionalVersion = 0
            )
        )
    }

    @Test
    fun GIVEN_fourth_part_WHEN_to_string_THEN_zero_is_omitted_and_non_zero_is_kept() {
        assertEquals(
            expected = "6.3.0",
            actual = SemVer(
                majorVersion = 6,
                minorVersion = 3,
                patchVersion = 0,
                additionalVersion = 0
            ).toString()
        )
        assertEquals(
            expected = "6.3.1",
            actual = SemVer(
                majorVersion = 6,
                minorVersion = 3,
                patchVersion = 1,
                additionalVersion = 0
            ).toString()
        )
        assertEquals(
            expected = "6.3.1.4",
            actual = SemVer(
                majorVersion = 6,
                minorVersion = 3,
                patchVersion = 1,
                additionalVersion = 4
            ).toString()
        )
    }
}
