package net.flipper.tools.drawtool.api.model

/**
 * Expected failures of a [net.flipper.tools.drawtool.api.DrawToolSyncApi.sync]
 * pass, carried inside `CResult.Failure` / [DrawToolSyncState.Failed].
 */
sealed class DrawToolSyncException(
    message: String,
    cause: Throwable?,
) : Exception(message, cause) {

    class BarNotConnected : DrawToolSyncException(
        message = "No connected bar with a reachable storage",
        cause = null,
    )

    /** The sync memory is keyed by serial number, so there is nothing to sync against. */
    class SerialNumberUnknown : DrawToolSyncException(
        message = "The connected bar has no serial number yet",
        cause = null,
    )

    /**
     * What succeeded stays synchronized; the next pass retries the rest.
     *
     * @param cause the first per-file failure
     */
    class PartiallyFailed(
        failedFilesCount: Int,
        cause: Throwable,
    ) : DrawToolSyncException(
        message = "Draw tool sync failed for $failedFilesCount files",
        cause = cause,
    )
}
