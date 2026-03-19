package net.flipper.bridge.connection.feature.rpc.api.model

import net.flipper.bridge.connection.feature.rpc.api.model.Fraction.Companion.fromWholePercent
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.error

/**
 * Represents a value normalized to the unit interval [0.0, 1.0]
 *
 * This type is used instead of raw numeric primitives to:
 * - enforce valid bounds at construction time
 * - avoid mixing different representations (e.g. 0..100 vs 0.0..1.0)
 *
 * ### Representation
 * Internally stored as a [Double] in range [0.0, 1.0].
 *
 * ### Conversions
 * - `0.0`   → 0%
 * - `1.0`   → 100%
 * - `0.42`  → 42%
 *
 * Use [fromWholePercent] or [toWholePercent] when working with human-readable values.
 *
 * ### Example
 * ```
 * val volume = Fraction.fromPercent(75)
 * println(volume.value) // 0.75
 * ```
 *
 * ### Why not use Double directly?
 * Using a dedicated type prevents:
 * - accidentally passing 75 instead of 0.75
 * - invalid values like 1.5 or -0.2
 * - ambiguity in APIs
 */
@ConsistentCopyVisibility
data class Fraction private constructor(
    private val value: Double
) : LogTagProvider by TaggedLogger("Fraction") {

    init {
        if (value !in MIN_FRACTION..MAX_FRACTION) {
            error { "#init Fraction not in range 0.0..1.0, got $value" }
        }
    }

    /**
     * Returns the raw fraction value in range [0.0, 1.0].
     */
    fun toDouble(): Double = value

    /**
     * Returns the raw fraction value in range [0.0, 1.0].
     */
    fun toFloat(): Float = value.toFloat()

    /**
     * Converts this fraction to an integer percentage [0.0; 100.0]
     */
    fun toWholePercent(): Double = value.times(MAX_PERCENT).coerceIn(MIN_FRACTION, MAX_PERCENT)

    companion object {
        private const val MIN_FRACTION = 0.0
        private const val MAX_FRACTION = 1.0
        private const val MAX_PERCENT = 100.0

        /** Represents 0% */
        val ZERO = Fraction(MIN_FRACTION)

        /** Represents 100% */
        val ONE = Fraction(MAX_FRACTION)

        /**
         * Creates a [Fraction] from a value already in range [0.0; 1.0]
         */
        fun fromFraction(value: Double): Fraction = Fraction(value)

        /**
         * Creates a [Fraction] from a value already in range [0.0; 1.0]
         */
        fun fromFraction(value: Float): Fraction = fromFraction(value.toDouble())

        /**
         * Creates a [Fraction] from a percentage value [0; 100.0]
         */
        fun fromWholePercent(percent: Double): Fraction {
            return Fraction(percent.div(MAX_PERCENT).coerceIn(MIN_FRACTION, MAX_FRACTION))
        }

        /**
         * Creates a [Fraction] from an integer percentage [0; 100]
         */
        fun fromWholePercent(percent: Int): Fraction = fromWholePercent(percent.toDouble())
    }
}
