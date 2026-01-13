package net.flipper.bridge.connection.feature.firmwareupdate.updater.model

import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbVersionChangelog

sealed interface FwUpdateState {
    data object Failure : FwUpdateState
    data object CheckingVersion : FwUpdateState
    data object LowBattery : FwUpdateState
    data object Busy : FwUpdateState
    object Pending : FwUpdateState
    object CouldNotCheckUpdate : FwUpdateState
    object NoUpdateAvailable : FwUpdateState
    data class UpdateFinished(val bsbVersionChangelog: BsbVersionChangelog?) : FwUpdateState
    data class Updating(val bsbVersionChangelog: BsbVersionChangelog?) : FwUpdateState
    data class UpdateAvailable(
        val bsbVersionChangelog: BsbVersionChangelog?
    ) : FwUpdateState

    data class Downloading(
        val bsbVersionChangelog: BsbVersionChangelog?,
        val progress: Float
    ) : FwUpdateState
}
