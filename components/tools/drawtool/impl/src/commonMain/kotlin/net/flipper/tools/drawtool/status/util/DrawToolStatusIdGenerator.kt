package net.flipper.tools.drawtool.status.util

import dev.zacsweers.metro.Inject
import net.flipper.tools.drawtool.collection.util.DrawToolStatusIdValidator
import kotlin.random.Random
import kotlin.random.nextULong

/**
 * Generates status ids. [random] and [maxAttempts] are parameters so a test can
 * feed a fixed sequence and drive the collision and exhaustion paths.
 */
@Inject
class DrawToolStatusIdGenerator(
    private val random: Random = Random.Default,
    private val radix: Int = HEX_RADIX,
    private val maxAttempts: Int = GENERATION_MAX_ATTEMPTS
) {
    /** 64 random bits as 16 lowercase hex characters, left-padded with zeroes. */
    fun generate(): String {
        return random
            .nextULong()
            .toString(radix)
            .padStart(DrawToolStatusIdValidator.STATUS_ID_LENGTH, '0')
    }

    /** An id that collides with none of [existingStatusIds], retried on collision. */
    fun generateFree(existingStatusIds: Collection<String>): Result<String> {
        val existingIds = existingStatusIds.toSet()
        repeat(maxAttempts) {
            val candidateId = generate()
            if (candidateId !in existingIds) return Result.success(candidateId)
        }
        return Result.failure(
            IllegalStateException("Could not generate a free Draw tool status id")
        )
    }

    companion object {
        private const val HEX_RADIX = 16

        /**
         * A collision in 64 bits is already vanishingly unlikely, so this is
         * only here to keep a broken [Random] from looping forever.
         */
        private const val GENERATION_MAX_ATTEMPTS = 100
    }
}
