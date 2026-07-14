package net.flipper.bridge.connection.feature.firmwareupdate.util

import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent.Update.UpdateState
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus

/**
 * Failure statuses reported by the device. `DOWNLOAD_ABORT` is a user-initiated stop, not an
 * error; `OK`/`BATTERY_LOW`/`BUSY` are not failures either.
 */
private fun UpdateState.BsbStatus.toFailedOrNull(): BsbUpdateStatus.Failed? = when (this) {
    UpdateState.BsbStatus.DOWNLOAD_FAILURE,
    UpdateState.BsbStatus.SHA_MISMATCH,
    UpdateState.BsbStatus.UNPACK_STAGING_DIR_FAILURE,
    UpdateState.BsbStatus.UNPACK_ARCHIVE_OPEN_FAILURE,
    UpdateState.BsbStatus.UNPACK_ARCHIVE_UNPACK_FAILURE,
    UpdateState.BsbStatus.INSTALL_MANIFEST_NOT_FOUND,
    UpdateState.BsbStatus.INSTALL_MANIFEST_INVALID,
    UpdateState.BsbStatus.INSTALL_SESSION_CONFIG_FAILURE,
    UpdateState.BsbStatus.INSTALL_POINTER_SETUP_FAILURE,
    UpdateState.BsbStatus.UNKNOWN_FAILURE -> BsbUpdateStatus.Failed

    UpdateState.BsbStatus.OK,
    UpdateState.BsbStatus.BATTERY_LOW,
    UpdateState.BsbStatus.BUSY,
    UpdateState.BsbStatus.DOWNLOAD_ABORT -> null
}

private fun UpdateStatus.Install.Status.toFailedOrNull(): BsbUpdateStatus.Failed? = when (this) {
    UpdateStatus.Install.Status.DOWNLOAD_FAILURE,
    UpdateStatus.Install.Status.SHA_MISMATCH,
    UpdateStatus.Install.Status.UNPACK_STAGING_DIR_FAILURE,
    UpdateStatus.Install.Status.UNPACK_ARCHIVE_OPEN_FAILURE,
    UpdateStatus.Install.Status.UNPACK_ARCHIVE_UNPACK_FAILURE,
    UpdateStatus.Install.Status.INSTALL_MANIFEST_NOT_FOUND,
    UpdateStatus.Install.Status.INSTALL_MANIFEST_INVALID,
    UpdateStatus.Install.Status.INSTALL_SESSION_CONFIG_FAILURE,
    UpdateStatus.Install.Status.INSTALL_POINTER_SETUP_FAILURE,
    UpdateStatus.Install.Status.UNKNOWN_FAILURE -> BsbUpdateStatus.Failed

    UpdateStatus.Install.Status.OK,
    UpdateStatus.Install.Status.BATTERY_LOW,
    UpdateStatus.Install.Status.BUSY,
    UpdateStatus.Install.Status.DOWNLOAD_ABORT -> null
}

@Suppress("CyclomaticComplexMethod")
internal fun UpdateState.toBsbUpdateStatus(): BsbUpdateStatus {
    status.toFailedOrNull()?.let { return it }
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
    install.status.toFailedOrNull()?.let { return it }
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
