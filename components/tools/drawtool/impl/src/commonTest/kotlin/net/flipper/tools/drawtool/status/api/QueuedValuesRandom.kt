package net.flipper.tools.drawtool.status.api

import kotlin.random.Random

/**
 * Returns the given 64-bit values in order and then repeats the last one, so a
 * test can pin the ends of the id range and a run of colliding candidates.
 *
 * The id generator only draws [nextLong]; [nextBits] is derived from the same
 * value so the fake cannot disagree with itself if that changes.
 */
class QueuedValuesRandom(private val values: List<ULong>) : Random() {
    private var drawCount = 0

    init {
        require(values.isNotEmpty()) { "A random needs at least one value to return" }
    }

    override fun nextLong(): Long {
        val value = values[minOf(drawCount, values.lastIndex)]
        drawCount++
        return value.toLong()
    }

    override fun nextBits(bitCount: Int): Int {
        return (nextLong() ushr (Long.SIZE_BITS - bitCount)).toInt()
    }
}
