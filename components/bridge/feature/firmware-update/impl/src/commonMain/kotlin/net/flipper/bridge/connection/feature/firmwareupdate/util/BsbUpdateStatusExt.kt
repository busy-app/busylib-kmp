package net.flipper.bridge.connection.feature.firmwareupdate.util

import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent.Update.UpdateState
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.FailedUpdate
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus.Install.Status
import kotlin.Int

internal fun BusyLibUpdateEvent.Update.UpdateDownload.toBsbUpdateStatus(): BsbUpdateStatus.InProgress.Downloading.Specified {
    return BsbUpdateStatus.InProgress.Downloading.Specified(
        speedBytesPerSec = speedBytesPerSec,
        receivedBytes = receivedBytes,
        totalBytes = totalBytes
    )
}

internal fun UpdateState.toBsbUpdateStatus(): BsbUpdateStatus {
    val failReason = when (status) {
        UpdateState.BsbStatus.OK,
        UpdateState.BsbStatus.BUSY, // Update in progress
        UpdateState.BsbStatus.BATTERY_LOW,
        UpdateState.BsbStatus.DOWNLOAD_ABORT -> null

        UpdateState.BsbStatus.DOWNLOAD_FAILURE -> FailedUpdate.Reason.DOWNLOAD_FAILURE
        UpdateState.BsbStatus.SHA_MISMATCH -> FailedUpdate.Reason.SHA_MISMATCH
        UpdateState.BsbStatus.UNPACK_STAGING_DIR_FAILURE -> FailedUpdate.Reason.UNPACK_STAGING_DIR_FAILURE
        UpdateState.BsbStatus.UNPACK_ARCHIVE_OPEN_FAILURE -> FailedUpdate.Reason.UNPACK_ARCHIVE_OPEN_FAILURE
        UpdateState.BsbStatus.UNPACK_ARCHIVE_UNPACK_FAILURE -> FailedUpdate.Reason.UNPACK_ARCHIVE_UNPACK_FAILURE
        UpdateState.BsbStatus.INSTALL_MANIFEST_NOT_FOUND -> FailedUpdate.Reason.INSTALL_MANIFEST_NOT_FOUND
        UpdateState.BsbStatus.INSTALL_MANIFEST_INVALID -> FailedUpdate.Reason.INSTALL_MANIFEST_INVALID
        UpdateState.BsbStatus.INSTALL_SESSION_CONFIG_FAILURE -> FailedUpdate.Reason.INSTALL_SESSION_CONFIG_FAILURE
        UpdateState.BsbStatus.INSTALL_POINTER_SETUP_FAILURE -> FailedUpdate.Reason.INSTALL_POINTER_SETUP_FAILURE
        UpdateState.BsbStatus.UNKNOWN_FAILURE -> FailedUpdate.Reason.UNKNOWN_FAILURE
    }
    if (failReason != null) {
        return FailedUpdate(failReason)
    }
    return when (action) {
        UpdateState.BsbAction.DOWNLOAD -> BsbUpdateStatus.InProgress.Downloading.NotSpecified
        UpdateState.BsbAction.SHA_VERIFICATION -> BsbUpdateStatus.InProgress.Other(BsbUpdateStatus.InProgress.Other.ProgressStage.SHA_VERIFICATION)
        UpdateState.BsbAction.UNPACK -> BsbUpdateStatus.InProgress.Other(BsbUpdateStatus.InProgress.Other.ProgressStage.UNPACK)
        UpdateState.BsbAction.PREPARE -> BsbUpdateStatus.InProgress.Other(BsbUpdateStatus.InProgress.Other.ProgressStage.PREPARE)
        UpdateState.BsbAction.APPLY -> BsbUpdateStatus.InProgress.Other(BsbUpdateStatus.InProgress.Other.ProgressStage.APPLY)
        UpdateState.BsbAction.NONE -> if (status == UpdateState.BsbStatus.BATTERY_LOW) {
            BsbUpdateStatus.ReadyToInstall.BatteryLow
        } else {
            BsbUpdateStatus.ReadyToInstall.Ready
        }
    }
}

internal fun UpdateStatus.toBsbUpdateStatus(): BsbUpdateStatus {
    val failReason = when (install.status) {
        Status.OK,
        Status.BUSY,
        Status.BATTERY_LOW,
        Status.DOWNLOAD_ABORT -> null

        Status.DOWNLOAD_FAILURE -> FailedUpdate.Reason.DOWNLOAD_FAILURE
        Status.SHA_MISMATCH -> FailedUpdate.Reason.SHA_MISMATCH
        Status.UNPACK_STAGING_DIR_FAILURE -> FailedUpdate.Reason.UNPACK_STAGING_DIR_FAILURE
        Status.UNPACK_ARCHIVE_OPEN_FAILURE -> FailedUpdate.Reason.UNPACK_ARCHIVE_OPEN_FAILURE
        Status.UNPACK_ARCHIVE_UNPACK_FAILURE -> FailedUpdate.Reason.UNPACK_ARCHIVE_UNPACK_FAILURE
        Status.INSTALL_MANIFEST_NOT_FOUND -> FailedUpdate.Reason.INSTALL_MANIFEST_NOT_FOUND
        Status.INSTALL_MANIFEST_INVALID -> FailedUpdate.Reason.INSTALL_MANIFEST_INVALID
        Status.INSTALL_SESSION_CONFIG_FAILURE -> FailedUpdate.Reason.INSTALL_SESSION_CONFIG_FAILURE
        Status.INSTALL_POINTER_SETUP_FAILURE -> FailedUpdate.Reason.INSTALL_POINTER_SETUP_FAILURE
        Status.UNKNOWN_FAILURE -> FailedUpdate.Reason.UNKNOWN_FAILURE
    }
    if (failReason != null) {
        return FailedUpdate(failReason)
    }
    return when (install.action) {
        UpdateStatus.Install.Action.DOWNLOAD -> BsbUpdateStatus.InProgress.Downloading.Specified(
            speedBytesPerSec = install.download.speedBytesPerSec,
            totalBytes = install.download.totalBytes,
            receivedBytes = install.download.receivedBytes
        )

        UpdateStatus.Install.Action.SHA_VERIFICATION -> BsbUpdateStatus.InProgress.Other(
            BsbUpdateStatus.InProgress.Other.ProgressStage.SHA_VERIFICATION
        )

        UpdateStatus.Install.Action.UNPACK -> BsbUpdateStatus.InProgress.Other(BsbUpdateStatus.InProgress.Other.ProgressStage.UNPACK)
        UpdateStatus.Install.Action.PREPARE -> BsbUpdateStatus.InProgress.Other(BsbUpdateStatus.InProgress.Other.ProgressStage.PREPARE)
        UpdateStatus.Install.Action.APPLY -> BsbUpdateStatus.InProgress.Other(BsbUpdateStatus.InProgress.Other.ProgressStage.APPLY)
        UpdateStatus.Install.Action.NONE -> if (install.status == Status.BATTERY_LOW || !install.isAllowed) {
            BsbUpdateStatus.ReadyToInstall.BatteryLow
        } else {
            BsbUpdateStatus.ReadyToInstall.Ready
        }
    }
}
