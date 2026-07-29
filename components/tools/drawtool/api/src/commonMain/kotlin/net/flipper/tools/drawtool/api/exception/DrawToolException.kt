package net.flipper.tools.drawtool.api.exception

import kotlinx.io.files.Path

/** Base of every expected Draw tool failure reported via `CResult.Failure`. */
sealed class DrawToolException(message: String) : Exception(message)

/**
 * The status collection of the current BUSY Bar cannot be resolved: no bar is
 * selected, or its serial number is not known yet — it is learned on the first
 * successful connection.
 */
class DrawToolCollectionUnavailableException(
    message: String
) : DrawToolException(message)

/** [statusId] is not exactly 16 lowercase hex characters. */
class DrawToolInvalidStatusIdException(
    val statusId: String
) : DrawToolException("Draw tool status id '$statusId' must be 16 lowercase hex characters")

/**
 * The given status files are invalid: no files at all, no frame, duplicate or
 * malformed relative paths, a reserved file name, or a path that contradicts
 * the declared file type.
 */
class DrawToolInvalidFileException(
    message: String
) : DrawToolException(message)

/**
 * [relativePath] does not fit the budget left by the 63 character path limit of
 * the BUSY Bar storage API.
 */
class DrawToolPathTooLongException(
    val relativePath: Path,
    val maxLength: Int
) : DrawToolException(
    "Draw tool file path '$relativePath' is longer than $maxLength characters"
)
