package net.flipper.tools.drawtool.sync.model

/**
 * What one pass has to do, in status names; touches nothing itself.
 *
 * @param uploadToBar new local statuses the bar has never seen
 * @param downloadFromBar new bar statuses the client has never seen
 * @param deleteLocally statuses deleted on the bar or half-deleted locally: tombstoned first, then deleted
 * @param deleteFromBar tombstoned statuses the bar still lists
 * @param markInSync names present on both sides — never touched, only remembered as synced
 * @param isBarReset the bar lost its whole collection although statuses were synchronized
 * with it before (a factory reset, not a deletion of everything): its sync memory is
 * dropped and [uploadToBar] re-fills it
 */
data class DrawToolSyncPlan(
    val uploadToBar: Set<DrawToolStatusName>,
    val downloadFromBar: Set<DrawToolStatusName>,
    val deleteLocally: Set<DrawToolStatusName>,
    val deleteFromBar: Set<DrawToolStatusName>,
    val markInSync: Set<DrawToolStatusName>,
    val isBarReset: Boolean,
)
