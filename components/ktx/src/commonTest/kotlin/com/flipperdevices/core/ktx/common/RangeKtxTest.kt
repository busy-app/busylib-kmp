package com.flipperdevices.core.ktx.common

import kotlin.test.Test
import kotlin.test.assertEquals

class RangeKtxTest {
    @Test
    fun GIVEN_minimum_WHEN_range_ten_times_greater_THEN_minimum() {
        assertEquals(
            expected = 0f,
            actual = mapValueToRange(
                oldRange = 0f..10f,
                newRange = 0f..100f,
                value = 0f
            ).getOrThrow()
        )
    }

    @Test
    fun GIVEN_maximum_WHEN_range_ten_times_greater_THEN_maximum() {
        assertEquals(
            expected = 100f,
            actual = mapValueToRange(
                oldRange = 0f..10f,
                newRange = 0f..100f,
                value = 10f
            ).getOrThrow()
        )
    }

    @Test
    fun GIVEN_mean_WHEN_range_ten_times_greater_THEN_mean() {
        assertEquals(
            expected = 50f,
            actual = mapValueToRange(
                oldRange = 0f..10f,
                newRange = 0f..100f,
                value = 5f
            ).getOrThrow()
        )
    }

    @Test
    fun GIVEN_mean_WHEN_range_ten_times_greater_and_mirrored_THEN_minus_mean() {
        assertEquals(
            expected = -50f,
            actual = mapValueToRange(
                oldRange = 0f..10f,
                newRange = -100f..0f,
                value = 5f
            ).getOrThrow()
        )
    }
}
