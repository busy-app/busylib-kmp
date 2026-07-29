package net.flipper.tools.drawtool.status.api

import net.flipper.tools.drawtool.status.util.DrawToolStatusIdGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A free id is one no status directory in the collection already uses: two
 * statuses sharing a directory name would overwrite each other.
 */
class DrawToolStatusIdGeneratorTest {
    @Test
    fun GIVEN_no_existing_ids_WHEN_free_id_generated_THEN_returns_the_first_candidate() {
        val generator = DrawToolStatusIdGenerator(random = QueuedValuesRandom(listOf(1uL)))

        val statusId = generator.generateFree(emptyList())

        assertEquals("0000000000000001", statusId.getOrNull())
    }

    @Test
    fun GIVEN_first_candidate_is_taken_WHEN_free_id_generated_THEN_returns_the_next_free_candidate() {
        val generator = DrawToolStatusIdGenerator(
            random = QueuedValuesRandom(listOf(1uL, 2uL))
        )

        val statusId = generator.generateFree(listOf("0000000000000001"))

        assertEquals("0000000000000002", statusId.getOrNull())
    }

    /**
     * Giving up must be reported rather than returning a colliding id, which
     * would silently overwrite the status that already owns the directory.
     */
    @Test
    fun GIVEN_every_candidate_is_taken_WHEN_free_id_generated_THEN_fails() {
        val generator = DrawToolStatusIdGenerator(
            random = QueuedValuesRandom(listOf(1uL)),
            maxAttempts = 3
        )

        val result = generator.generateFree(listOf("0000000000000001"))

        assertNull(result.getOrNull())
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    /**
     * An id is only free with respect to the ids it was compared against, so a
     * generator that cannot make a single attempt must fail instead of
     * returning an unchecked id.
     */
    @Test
    fun GIVEN_no_attempts_allowed_WHEN_free_id_generated_THEN_fails_without_drawing() {
        val random = QueuedValuesRandom(listOf(1uL))
        val generator = DrawToolStatusIdGenerator(random = random, maxAttempts = 0)

        val result = generator.generateFree(emptyList())

        assertTrue(result.isFailure)
        assertEquals("0000000000000001", generator.generate())
    }

    /** Case is part of an id, so an uppercase name never blocks a candidate. */
    @Test
    fun GIVEN_existing_ids_of_other_case_WHEN_free_id_generated_THEN_they_do_not_collide() {
        val generator = DrawToolStatusIdGenerator(
            random = QueuedValuesRandom(listOf(0xABCDEF0123456789uL)),
            maxAttempts = 1
        )

        val statusId = generator.generateFree(listOf("ABCDEF0123456789"))

        assertEquals("abcdef0123456789", statusId.getOrNull())
    }
}
