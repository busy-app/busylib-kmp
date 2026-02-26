package net.flipper.bridge.device.firmwareupdate.updater.model

sealed interface FwUpdateState {
    data object Failure : FwUpdateState
    data object CheckingVersion : FwUpdateState
    data object LowBattery : FwUpdateState
    data object Busy : FwUpdateState
    data object Pending : FwUpdateState
    data object CouldNotCheckUpdate : FwUpdateState
    data object NoUpdateAvailable : FwUpdateState

    data object UpdateFinished : FwUpdateState

    data object UpdateFailed : FwUpdateState

    data object Updating : FwUpdateState

    data object UpdateAvailable : FwUpdateState

    data class Uploading(
        val progress: Float
    ) : FwUpdateState

    data class Downloading(
        val progress: Float
    ) : FwUpdateState
}
