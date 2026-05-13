package net.flipper.bridge.device.firmwareupdate.updater.model

sealed interface FwUpdateState {
    // Retrieving data
    data object Pending : FwUpdateState

    data object CheckingVersion : FwUpdateState
    data object LowBattery : FwUpdateState
    data object CouldNotCheckUpdate : FwUpdateState
    data object NoUpdateAvailable : FwUpdateState
    data object Updating : FwUpdateState
    data object UpdateAvailable : FwUpdateState

    // Desktop only
    data class Uploading(
        val progress: Float
    ) : FwUpdateState

    data class Downloading(
        val progress: Float,
        val isLanUpdate: Boolean
    ) : FwUpdateState

    data object DownloadFailure : FwUpdateState
}
