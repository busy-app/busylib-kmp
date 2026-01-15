package net.flipper.bridge.connection.feature.firmwareupdate.updater.api

import net.flipper.bridge.connection.feature.firmwareupdate.updater.model.FwUpdateState

object FwUpdateStateDiff {
    fun combineDiff(previous: FwUpdateState?, latest: FwUpdateState): FwUpdateState {
        return when (previous) {
            null -> latest

            is FwUpdateState.Updating,
            is FwUpdateState.UpdateFinished,
            is FwUpdateState.UpdateAvailable,
            FwUpdateState.Pending,
            FwUpdateState.NoUpdateAvailable,
            FwUpdateState.LowBattery,
            FwUpdateState.Failure,
            FwUpdateState.CouldNotCheckUpdate,
            FwUpdateState.CheckingVersion,
            FwUpdateState.Busy -> latest

            is FwUpdateState.Downloading -> {
                when (latest) {
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
                        FwUpdateState.Updating(previous.bsbVersionChangelog)
                    }
                }
            }
        }
    }
}
