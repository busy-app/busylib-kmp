package net.flipper.bridge.connection.feature.firmwareupdate.model

import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall.BsbStatus
import net.flipper.bridge.connection.feature.rpc.generated.model.UpdateStatus
import net.flipper.bridge.connection.feature.rpc.generated.model.UpdateStatusCheck
import net.flipper.bridge.connection.feature.rpc.generated.model.UpdateStatusInstall
import net.flipper.bridge.connection.feature.rpc.generated.model.UpdateStatusInstallDownload

private fun UpdateStatusInstall.Action.toBsbAction(): BsbUpdateStatus.BsbInstall.BsbAction {
    return when (this) {
        UpdateStatusInstall.Action.DOWNLOAD -> BsbUpdateStatus.BsbInstall.BsbAction.DOWNLOAD
        UpdateStatusInstall.Action.SHA_VERIFICATION -> BsbUpdateStatus.BsbInstall.BsbAction.SHA_VERIFICATION
        UpdateStatusInstall.Action.UNPACK -> BsbUpdateStatus.BsbInstall.BsbAction.UNPACK
        UpdateStatusInstall.Action.PREPARE -> BsbUpdateStatus.BsbInstall.BsbAction.PREPARE
        UpdateStatusInstall.Action.APPLY -> BsbUpdateStatus.BsbInstall.BsbAction.APPLY
        UpdateStatusInstall.Action.NONE -> BsbUpdateStatus.BsbInstall.BsbAction.NONE
    }
}

private fun UpdateStatusInstall.Status.toBsbStatus(): BsbStatus {
    return when (this) {
        UpdateStatusInstall.Status.OK -> BsbStatus.OK
        UpdateStatusInstall.Status.BATTERY_LOW -> BsbStatus.BATTERY_LOW
        UpdateStatusInstall.Status.BUSY -> BsbStatus.BUSY
        UpdateStatusInstall.Status.DOWNLOAD_FAILURE -> BsbStatus.DOWNLOAD_FAILURE
        UpdateStatusInstall.Status.DOWNLOAD_ABORT -> BsbStatus.DOWNLOAD_ABORT
        UpdateStatusInstall.Status.SHA_MISMATCH -> BsbStatus.SHA_MISMATCH
        UpdateStatusInstall.Status.UNPACK_STAGING_DIR_FAILURE -> BsbStatus.UNPACK_STAGING_DIR_FAILURE
        UpdateStatusInstall.Status.UNPACK_ARCHIVE_OPEN_FAILURE -> BsbStatus.UNPACK_ARCHIVE_OPEN_FAILURE
        UpdateStatusInstall.Status.UNPACK_ARCHIVE_UNPACK_FAILURE -> BsbStatus.UNPACK_ARCHIVE_UNPACK_FAILURE
        UpdateStatusInstall.Status.INSTALL_MANIFEST_NOT_FOUND -> BsbStatus.INSTALL_MANIFEST_NOT_FOUND
        UpdateStatusInstall.Status.INSTALL_MANIFEST_INVALID -> BsbStatus.INSTALL_MANIFEST_INVALID
        UpdateStatusInstall.Status.INSTALL_SESSION_CONFIG_FAILURE -> BsbStatus.INSTALL_SESSION_CONFIG_FAILURE
        UpdateStatusInstall.Status.INSTALL_POINTER_SETUP_FAILURE -> BsbStatus.INSTALL_POINTER_SETUP_FAILURE
        UpdateStatusInstall.Status.UNKNOWN_FAILURE -> BsbStatus.UNKNOWN_FAILURE
    }
}

private fun UpdateStatusInstallDownload.toBsbDownload(): BsbUpdateStatus.BsbInstall.BsbDownload {
    return BsbUpdateStatus.BsbInstall.BsbDownload(
        speedBytesPerSec = speedBytesPerSec,
        receivedBytes = receivedBytes,
        totalBytes = totalBytes
    )
}

private fun UpdateStatusInstall.toBsbInstall(): BsbUpdateStatus.BsbInstall {
    return BsbUpdateStatus.BsbInstall(
        isAllowed = isAllowed,
        action = action.toBsbAction(),
        status = status.toBsbStatus(),
        download = download.toBsbDownload()
    )
}

private fun UpdateStatusCheck.Status.toBsbCheckResult(): BsbUpdateStatus.BsbCheck.BsbCheckResult {
    return when (this) {
        UpdateStatusCheck.Status.AVAILABLE -> BsbUpdateStatus.BsbCheck.BsbCheckResult.AVAILABLE
        UpdateStatusCheck.Status.NOT_AVAILABLE -> BsbUpdateStatus.BsbCheck.BsbCheckResult.NOT_AVAILABLE
        UpdateStatusCheck.Status.FAILURE -> BsbUpdateStatus.BsbCheck.BsbCheckResult.FAILURE
        UpdateStatusCheck.Status.NONE -> BsbUpdateStatus.BsbCheck.BsbCheckResult.NONE
    }
}

private fun UpdateStatusCheck.toBsbCheck(): BsbUpdateStatus.BsbCheck {
    return BsbUpdateStatus.BsbCheck(
        availableVersion = availableVersion,
        status = status.toBsbCheckResult()
    )
}

internal fun UpdateStatus.toBsbUpdateStatus(): BsbUpdateStatus {
    return BsbUpdateStatus(
        install = install.toBsbInstall(),
        check = check.toBsbCheck()
    )
}
