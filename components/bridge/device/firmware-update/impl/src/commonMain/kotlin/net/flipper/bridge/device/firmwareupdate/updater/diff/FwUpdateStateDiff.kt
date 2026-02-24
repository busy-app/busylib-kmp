package net.flipper.bridge.device.firmwareupdate.updater.diff

import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarVersion
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState

object FwUpdateStateDiff {
    fun combineDiff(
        previous: FwUpdateState?,
        latest: FwUpdateState,
        currentVersion: BusyBarVersion,
        previousVersion: BusyBarVersion?
    ): FwUpdateState {
        return when (previous) {
            null -> latest
            is FwUpdateState.UpdateFailed,
            is FwUpdateState.UpdateFinished,
            is FwUpdateState.UpdateAvailable,
            FwUpdateState.Pending,
            FwUpdateState.NoUpdateAvailable,
            FwUpdateState.LowBattery,
            FwUpdateState.Failure,
            FwUpdateState.CouldNotCheckUpdate,
            FwUpdateState.CheckingVersion,
            FwUpdateState.Busy -> latest

            is FwUpdateState.Updating -> {
                if (currentVersion.version == previousVersion?.version) {
                    FwUpdateState.UpdateFinished
                } else {
                    FwUpdateState.UpdateFailed
                }
            }

            is FwUpdateState.Uploading,
            is FwUpdateState.Downloading -> {
                when (latest) {
                    is FwUpdateState.Uploading,
                    is FwUpdateState.UpdateFailed,
                    is FwUpdateState.UpdateFinished,
                    FwUpdateState.Busy,
                    FwUpdateState.CheckingVersion,
                    FwUpdateState.CouldNotCheckUpdate,
                    is FwUpdateState.Downloading,
                    FwUpdateState.Failure,
                    FwUpdateState.LowBattery,
                    FwUpdateState.NoUpdateAvailable,
                    is FwUpdateState.Updating,
                    is FwUpdateState.UpdateAvailable -> latest

                    FwUpdateState.Pending -> {
                        FwUpdateState.Updating
                    }
                }
            }
        }
    }
}
