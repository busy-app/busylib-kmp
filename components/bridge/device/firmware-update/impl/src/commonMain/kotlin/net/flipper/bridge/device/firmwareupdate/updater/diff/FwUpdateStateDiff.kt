package net.flipper.bridge.device.firmwareupdate.updater.diff

import net.flipper.bridge.device.firmwareupdate.updater.model.BusyBarVersionTransition
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateEvent
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState

internal object FwUpdateStateDiff {
    fun compareAndGetEvent(
        fwUpdateState: FwUpdateState?,
        busyBarVersionTransition: BusyBarVersionTransition?,
    ): FwUpdateEvent? {
        return when (fwUpdateState) {
            is FwUpdateState.UpdateAvailable,
            FwUpdateState.Pending,
            FwUpdateState.NoUpdateAvailable,
            FwUpdateState.LowBattery,
            FwUpdateState.DownloadFailure,
            FwUpdateState.Failure,
            FwUpdateState.CouldNotCheckUpdate,
            FwUpdateState.CheckingVersion,
            FwUpdateState.Busy -> {
                @Suppress("UseLet")
                if (busyBarVersionTransition == null) {
                    null
                } else if (busyBarVersionTransition.previousVersion == null) {
                    null
                } else if (busyBarVersionTransition.currentVersion == busyBarVersionTransition.previousVersion) {
                    FwUpdateEvent.UpdateFinished
                } else {
                    FwUpdateEvent.UpdateFailed
                }
            }

            null -> null

            is FwUpdateState.Updating,
            is FwUpdateState.Uploading,
            is FwUpdateState.Downloading -> null
        }
    }
}
