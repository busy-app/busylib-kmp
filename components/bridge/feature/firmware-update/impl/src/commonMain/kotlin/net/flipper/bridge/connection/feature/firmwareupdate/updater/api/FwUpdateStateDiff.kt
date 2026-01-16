package net.flipper.bridge.connection.feature.firmwareupdate.updater.api

import net.flipper.bridge.connection.feature.firmwareupdate.updater.model.FwUpdateState

object FwUpdateStateDiff {
    suspend fun combineDiff(
        previous: FwUpdateState?,
        latest: FwUpdateState,
        getCurrentVersion: suspend () -> String,
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
                val currentVersion = getCurrentVersion.invoke()
                if (currentVersion == previous.targetVersion) {
                    FwUpdateState.UpdateFinished(
                        previous.targetVersion,
                        previous.bsbVersionChangelog
                    )
                } else {
                    FwUpdateState.UpdateFailed(
                        targetVersion = previous.targetVersion
                    )
                }
            }

            is FwUpdateState.Downloading -> {
                when (latest) {
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
                        FwUpdateState.Updating(
                            targetVersion = previous.targetVersion,
                            bsbVersionChangelog = previous.bsbVersionChangelog
                        )
                    }
                }
            }
        }
    }
}
