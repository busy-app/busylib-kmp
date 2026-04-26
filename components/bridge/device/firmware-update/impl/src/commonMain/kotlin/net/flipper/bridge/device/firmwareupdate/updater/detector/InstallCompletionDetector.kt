package net.flipper.bridge.device.firmwareupdate.updater.detector

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.scan
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall.BsbAction
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall.BsbStatus
import net.flipper.bridge.device.firmwareupdate.status.model.UpdateStatusSource

/**
 * Watches device-side update FSM and signals when install transitions back to
 * idle after being active. Used to detect update completion through whichever
 * transport survives a reboot (LAN or cloud), independent of the legacy
 * uptime-disconnect probe.
 *
 * Why scan-with-`sawActive`: the initial idle state must NOT trigger detection
 * — only the active→idle transition does.
 */
internal object InstallCompletionDetector {

    fun detect(statuses: Flow<UpdateStatusSource>): Flow<Unit> = statuses
        .mapNotNull { source -> source.freshUpdateStatus?.install }
        .scan(SeenState.Initial) { acc, install -> acc.advance(install) }
        .filter { state -> state.completed }
        .map { }

    private data class SeenState(val sawActive: Boolean, val completed: Boolean) {
        fun advance(install: BsbInstall): SeenState {
            val active = install.action != BsbAction.NONE
            val idleNow = install.action == BsbAction.NONE && install.status == BsbStatus.OK
            val sawActiveNext = sawActive || active
            return SeenState(
                sawActive = sawActiveNext,
                completed = sawActiveNext && idleNow
            )
        }

        companion object {
            val Initial = SeenState(sawActive = false, completed = false)
        }
    }
}
