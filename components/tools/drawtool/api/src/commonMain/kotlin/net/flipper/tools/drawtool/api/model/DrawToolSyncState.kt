package net.flipper.tools.drawtool.api.model

sealed interface DrawToolSyncState {
    data object Idle : DrawToolSyncState

    data object InProgress : DrawToolSyncState

    /** Files transferred before the failure stay synchronized. */
    data class Failed(val throwable: Throwable) : DrawToolSyncState
}
