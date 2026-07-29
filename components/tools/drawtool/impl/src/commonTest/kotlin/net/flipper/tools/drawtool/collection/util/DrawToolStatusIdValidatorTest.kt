package net.flipper.tools.drawtool.collection.util

import net.flipper.tools.drawtool.api.exception.DrawToolInvalidStatusIdException
import net.flipper.tools.drawtool.status.api.QueuedValuesRandom
import net.flipper.tools.drawtool.status.util.DrawToolStatusIdGenerator
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * A generated id and an accepted id must be the same thing: the validator
 * guards ids that come back from storage, and the generator is the only source
 * of new ones.
 */
class DrawToolStatusIdValidatorTest {
    private val validator = DrawToolStatusIdValidator()

    /**
     * Roughly one in sixteen draws is short in hex and has to be padded, so the
     * run reliably covers padded ids as well as full width ones.
     */
    @Test
    fun GIVEN_generated_ids_WHEN_validated_THEN_all_are_valid() {
        val generator = DrawToolStatusIdGenerator(random = Random(seed = GENERATOR_SEED))

        repeat(GENERATED_IDS_COUNT) { attempt ->
            val statusId = generator.generate()

            assertTrue(validator.isValid(statusId), "attempt $attempt: $statusId")
        }
    }

    /**
     * The ends of the random range are where the hex text changes width: a draw
     * of zero is one character wide and has to be padded to the fixed status
     * directory name, while the largest draw must still fit into it.
     */
    @Test
    fun GIVEN_random_values_across_the_range_WHEN_ids_generated_THEN_are_valid_ids_of_full_width() {
        val generator = DrawToolStatusIdGenerator(
            random = QueuedValuesRandom(listOf(0uL, 15uL, 1uL shl 60, ULong.MAX_VALUE))
        )
        val expectedIds = listOf(
            "0000000000000000",
            "000000000000000f",
            "1000000000000000",
            "ffffffffffffffff"
        )

        expectedIds.forEach { expectedId ->
            val statusId = generator.generate()

            assertEquals(expectedId, statusId)
            assertTrue(validator.isValid(statusId), statusId)
        }
    }

    @Test
    fun GIVEN_ids_of_wrong_length_WHEN_validated_THEN_are_invalid() {
        val wrongLengthIds = listOf("", "0", "abcdef012345678", "abcdef01234567890")

        wrongLengthIds.forEach { statusId ->
            assertFalse(validator.isValid(statusId), statusId)
        }
    }

    /**
     * The bar compares directory names literally, so an id that only differs
     * in case would address a different status than the one asked for.
     */
    @Test
    fun GIVEN_uppercase_hex_id_WHEN_validated_THEN_is_invalid() {
        assertFalse(validator.isValid("ABCDEF0123456789"))
        assertFalse(validator.isValid("abcdeF0123456789"))
    }

    @Test
    fun GIVEN_ids_of_non_hex_characters_WHEN_validated_THEN_are_invalid() {
        val nonHexIds = listOf(
            "abcdef012345678g",
            "abcdef012345678z",
            "abcdef01234567-9",
            "abcdef0123456 89",
            "abcdef012345\n789",
            "0xabcdef01234567",
            "абвгде0123456789"
        )

        nonHexIds.forEach { statusId ->
            assertFalse(validator.isValid(statusId), statusId)
        }
    }

    /**
     * Digits of other scripts are not hex digits however digit-like they look,
     * and a directory name read off the bar is not restricted to ASCII.
     */
    @Test
    fun GIVEN_ids_of_non_ascii_digits_WHEN_validated_THEN_are_invalid() {
        assertFalse(validator.isValid("٠١٢٣٤٥٦٧٨٩abcdef"))
        assertFalse(validator.isValid("０123456789abcdef"))
    }

    /**
     * Length counts UTF-16 code units, so eight astral characters pass for a
     * sixteen character id and only the character check can reject them.
     */
    @Test
    fun GIVEN_id_of_surrogate_pairs_WHEN_validated_THEN_is_invalid() {
        val astralId = "😀".repeat(8)

        assertEquals(DrawToolStatusIdValidator.STATUS_ID_LENGTH, astralId.length)
        assertFalse(validator.isValid(astralId))
    }

    /**
     * A path separator inside an id would escape the status directory, and it
     * fits the length budget, so length alone cannot be relied on.
     */
    @Test
    fun GIVEN_id_containing_a_path_separator_WHEN_validated_THEN_is_invalid() {
        assertFalse(validator.isValid("abcdef01/2345678"))
        assertFalse(validator.isValid("abcdef01\\2345678"))
        assertFalse(validator.isValid("../abcdef0123456"))
    }

    @Test
    fun GIVEN_generated_id_WHEN_validate_THEN_returns_the_same_id() {
        val statusId = DrawToolStatusIdGenerator(random = Random(seed = GENERATOR_SEED)).generate()

        assertEquals(statusId, validator.validate(statusId).getOrNull())
    }

    @Test
    fun GIVEN_invalid_id_WHEN_validate_THEN_fails_with_invalid_status_id_exception() {
        val statusId = "not-a-status-id"

        val error = validator.validate(statusId).exceptionOrNull()

        assertIs<DrawToolInvalidStatusIdException>(error)
        assertEquals(statusId, error.statusId)
    }

    companion object {
        private const val GENERATED_IDS_COUNT = 1_000
        private const val GENERATOR_SEED = 7
    }
}
