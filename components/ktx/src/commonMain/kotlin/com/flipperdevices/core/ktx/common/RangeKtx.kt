package com.flipperdevices.core.ktx.common

/**
 * Maps a value from one range to another.
 *
 * For example, a value of 5 from a range of 0–10 mapped to a new range of 0–100 would result in 50.
 *
 * @param oldMin The minimum value of the original range.
 * @param oldMax The maximum value of the original range.
 * @param newMin The minimum value of the new range.
 * @param newMax The maximum value of the new range.
 * @param value The value within the original range to be mapped.
 * @return The value mapped to the new range.
 */
fun mapValueToRange(
    oldMin: Float,
    oldMax: Float,
    newMin: Float,
    newMax: Float,
    value: Float
): Result<Float> = runCatching {
    check(value in oldMin..oldMax) { "Value $value must lie within the old range [$oldMin, $oldMax]." }
    check(oldMin < oldMax) { "Invalid old range: oldMin ($oldMin) must be less than oldMax ($oldMax)." }
    check(newMin < newMax) { "Invalid new range: newMin ($newMin) must be less than newMax ($newMax)." }

    val oldRange = oldMax - oldMin
    check(oldRange != 0f) { "Old range is zero (oldMin == oldMax), cannot perform mapping." }

    val newRange = newMax - newMin
    ((value - oldMin) * newRange) / oldRange + newMin
}

/**
 * Maps a value from one range to another.
 *
 * For example, a value of 5 from a range of 0–10 mapped to a new range of 0–100 would result in 50.
 *
 * @param oldRange The original range.
 * @param newRange The new range.
 * @param value The value within the original range to be mapped.
 * @return The value mapped to the new range.
 */
fun mapValueToRange(
    oldRange: ClosedFloatingPointRange<Float>,
    newRange: ClosedFloatingPointRange<Float>,
    value: Float
): Result<Float> = mapValueToRange(
    oldMin = oldRange.start,
    oldMax = oldRange.endInclusive,
    newMin = newRange.start,
    newMax = newRange.endInclusive,
    value = value
)
