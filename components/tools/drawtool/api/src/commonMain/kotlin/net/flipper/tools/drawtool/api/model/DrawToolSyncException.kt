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
     * @param failedOperationsCount failed steps of the pass, transfers and deletions alike
     * @param cause the first per-operation failure
     */
    class PartiallyFailed(
        failedOperationsCount: Int,
        cause: Throwable,
    ) : DrawToolSyncException(
        message = "Draw tool sync failed for $failedOperationsCount operations",
        cause = cause,
    )
}
