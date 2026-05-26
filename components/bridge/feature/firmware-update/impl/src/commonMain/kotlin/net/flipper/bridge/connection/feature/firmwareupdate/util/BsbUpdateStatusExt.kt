package net.flipper.bridge.connection.feature.firmwareupdate.util

import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent.Update.UpdateState
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus

@Suppress("CyclomaticComplexMethod")
internal fun UpdateState.toBsbUpdateStatus(): BsbUpdateStatus {
    val inProgress = when (event) {
        UpdateState.BsbEvent.SESSION_STOP,
        UpdateState.BsbEvent.NONE -> false

        UpdateState.BsbEvent.ACTION_DONE, // Action done, but not update
        UpdateState.BsbEvent.DETAIL_CHANGE,
        UpdateState.BsbEvent.SESSION_START,
        UpdateState.BsbEvent.ACTION_BEGIN,
        UpdateState.BsbEvent.ACTION_PROGRESS -> true
    }
    return if (inProgress) {
        when (action) {
            UpdateState.BsbAction.DOWNLOAD -> BsbUpdateStatus.InProgress.Downloading.NotSpecified
            UpdateState.BsbAction.SHA_VERIFICATION -> BsbUpdateStatus.InProgress.Other(
                BsbUpdateStatus.InProgress.Other.ProgressStage.SHA_VERIFICATION
            )

            UpdateState.BsbAction.UNPACK -> BsbUpdateStatus.InProgress.Other(
                BsbUpdateStatus.InProgress.Other.ProgressStage.UNPACK
            )

            UpdateState.BsbAction.PREPARE -> BsbUpdateStatus.InProgress.Other(
                BsbUpdateStatus.InProgress.Other.ProgressStage.PREPARE
            )

            UpdateState.BsbAction.APPLY -> BsbUpdateStatus.InProgress.Other(
                BsbUpdateStatus.InProgress.Other.ProgressStage.APPLY
            )

            UpdateState.BsbAction.NONE -> BsbUpdateStatus.ReadyToInstall.Ready
        }
    } else {
        BsbUpdateStatus.ReadyToInstall.Ready
    }
}

@Suppress("CyclomaticComplexMethod")
internal fun UpdateStatus.toBsbUpdateStatus(): BsbUpdateStatus {
    val inProgress = when (install.event) {
        UpdateStatus.Install.Event.SESSION_STOP,
        UpdateStatus.Install.Event.NONE -> false

        UpdateStatus.Install.Event.ACTION_DONE, // Action done, but not update
        UpdateStatus.Install.Event.DETAIL_CHANGE,
        UpdateStatus.Install.Event.SESSION_START,
        UpdateStatus.Install.Event.ACTION_BEGIN,
        UpdateStatus.Install.Event.ACTION_PROGRESS -> true
    }
    val readyStatus = if (install.isAllowed.not() && install.status == UpdateStatus.Install.Status.BATTERY_LOW) {
        BsbUpdateStatus.ReadyToInstall.BatteryLow
    } else {
        BsbUpdateStatus.ReadyToInstall.Ready
    }
    return if (inProgress) {
        when (install.action) {
            UpdateStatus.Install.Action.DOWNLOAD -> BsbUpdateStatus.InProgress.Downloading.Specified(
                speedBytesPerSec = install.download.speedBytesPerSec,
                totalBytes = install.download.totalBytes,
                receivedBytes = install.download.receivedBytes
            )

            UpdateStatus.Install.Action.SHA_VERIFICATION -> BsbUpdateStatus.InProgress.Other(
                BsbUpdateStatus.InProgress.Other.ProgressStage.SHA_VERIFICATION
            )

            UpdateStatus.Install.Action.UNPACK -> BsbUpdateStatus.InProgress.Other(
                BsbUpdateStatus.InProgress.Other.ProgressStage.UNPACK
            )

            UpdateStatus.Install.Action.PREPARE -> BsbUpdateStatus.InProgress.Other(
                BsbUpdateStatus.InProgress.Other.ProgressStage.PREPARE
            )

            UpdateStatus.Install.Action.APPLY -> BsbUpdateStatus.InProgress.Other(
                BsbUpdateStatus.InProgress.Other.ProgressStage.APPLY
            )

            UpdateStatus.Install.Action.NONE -> readyStatus
        }
    } else {
        readyStatus
    }
}
