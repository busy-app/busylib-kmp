package net.flipper.tools.drawtool.collection.util

import dev.zacsweers.metro.Inject
import net.flipper.tools.drawtool.api.exception.DrawToolInvalidStatusIdException

/**
 * A status id is 64 random bits as 16 lowercase hex characters — a full UUID
 * would not fit the 63 character path limit of the bar storage API.
 */
@Inject
class DrawToolStatusIdValidator {
    /** Whether [statusId] may name a status directory. */
    fun isValid(statusId: String): Boolean {
        return statusId.length == STATUS_ID_LENGTH &&
            statusId.all { char -> char in '0'..'9' || char in 'a'..'f' }
    }

    /**
     * [statusId] itself, or [DrawToolInvalidStatusIdException] — so a malformed
     * id fails before it reaches the filesystem.
     */
    fun validate(statusId: String): Result<String> {
        return if (isValid(statusId)) {
            Result.success(statusId)
        } else {
            Result.failure(DrawToolInvalidStatusIdException(statusId))
        }
    }

    companion object {
        /** Hex characters of an id, and so the length of a status directory name. */
        const val STATUS_ID_LENGTH = 16
    }
}
