package net.flipper.bridge.connection.feature.firmwareupdate.updater.model

import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbVersionChangelog

sealed interface FwUpdateState {
    data object Failure : FwUpdateState
    data object CheckingVersion : FwUpdateState
    data object LowBattery : FwUpdateState
    data object Busy : FwUpdateState
    data object Pending : FwUpdateState
    data object CouldNotCheckUpdate : FwUpdateState
    data object NoUpdateAvailable : FwUpdateState

    data class UpdateFinished(
        val targetVersion: String,
        val bsbVersionChangelog: BsbVersionChangelog?
    ) : FwUpdateState

    data class UpdateFailed(
        val targetVersion: String,
    ) : FwUpdateState

    data class Updating(
        val targetVersion: String,
        val bsbVersionChangelog: BsbVersionChangelog?
    ) : FwUpdateState

    data class UpdateAvailable(
        val targetVersion: String,
        val bsbVersionChangelog: BsbVersionChangelog?
    ) : FwUpdateState

    data class Downloading(
        val targetVersion: String,
        val bsbVersionChangelog: BsbVersionChangelog?,
        val progress: Float
    ) : FwUpdateState
}
