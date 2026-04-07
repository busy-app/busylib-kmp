package net.flipper.bridge.connection.feature.firmwareupdate.model

import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall.BsbStatus
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus.Install.Status

private fun UpdateStatus.Install.Action.toBsbAction(): BsbUpdateStatus.BsbInstall.BsbAction {
    return when (this) {
        UpdateStatus.Install.Action.DOWNLOAD -> BsbUpdateStatus.BsbInstall.BsbAction.DOWNLOAD
        UpdateStatus.Install.Action.SHA_VERIFICATION -> BsbUpdateStatus.BsbInstall.BsbAction.SHA_VERIFICATION
        UpdateStatus.Install.Action.UNPACK -> BsbUpdateStatus.BsbInstall.BsbAction.UNPACK
        UpdateStatus.Install.Action.PREPARE -> BsbUpdateStatus.BsbInstall.BsbAction.PREPARE
        UpdateStatus.Install.Action.APPLY -> BsbUpdateStatus.BsbInstall.BsbAction.APPLY
        UpdateStatus.Install.Action.NONE -> BsbUpdateStatus.BsbInstall.BsbAction.NONE
    }
}

private fun Status.toBsbStatus(): BsbStatus {
    return when (this) {
        Status.OK -> BsbStatus.OK
        Status.BATTERY_LOW -> BsbStatus.BATTERY_LOW
        Status.BUSY -> BsbStatus.BUSY
        Status.DOWNLOAD_FAILURE -> BsbStatus.DOWNLOAD_FAILURE
        Status.DOWNLOAD_ABORT -> BsbStatus.DOWNLOAD_ABORT
        Status.SHA_MISMATCH -> BsbStatus.SHA_MISMATCH
        Status.UNPACK_STAGING_DIR_FAILURE -> BsbStatus.UNPACK_STAGING_DIR_FAILURE
        Status.UNPACK_ARCHIVE_OPEN_FAILURE -> BsbStatus.UNPACK_ARCHIVE_OPEN_FAILURE
        Status.UNPACK_ARCHIVE_UNPACK_FAILURE -> BsbStatus.UNPACK_ARCHIVE_UNPACK_FAILURE
        Status.INSTALL_MANIFEST_NOT_FOUND -> BsbStatus.INSTALL_MANIFEST_NOT_FOUND
        Status.INSTALL_MANIFEST_INVALID -> BsbStatus.INSTALL_MANIFEST_INVALID
        Status.INSTALL_SESSION_CONFIG_FAILURE -> BsbStatus.INSTALL_SESSION_CONFIG_FAILURE
        Status.INSTALL_POINTER_SETUP_FAILURE -> BsbStatus.INSTALL_POINTER_SETUP_FAILURE
        Status.UNKNOWN_FAILURE -> BsbStatus.UNKNOWN_FAILURE
    }
}

private fun UpdateStatus.Install.Download.toBsbDownload(): BsbUpdateStatus.BsbInstall.BsbDownload {
    return BsbUpdateStatus.BsbInstall.BsbDownload(
        speedBytesPerSec = speedBytesPerSec,
        receivedBytes = receivedBytes,
        totalBytes = totalBytes
    )
}

private fun UpdateStatus.Install.toBsbInstall(): BsbUpdateStatus.BsbInstall {
    return BsbUpdateStatus.BsbInstall(
        isAllowed = isAllowed,
        action = action.toBsbAction(),
        status = status.toBsbStatus(),
        download = download.toBsbDownload()
    )
}

private fun UpdateStatus.Check.CheckResult.toBsbCheckResult(): BsbUpdateStatus.BsbCheck.BsbCheckResult {
    return when (this) {
        UpdateStatus.Check.CheckResult.AVAILABLE -> BsbUpdateStatus.BsbCheck.BsbCheckResult.AVAILABLE
        UpdateStatus.Check.CheckResult.NOT_AVAILABLE -> BsbUpdateStatus.BsbCheck.BsbCheckResult.NOT_AVAILABLE
        UpdateStatus.Check.CheckResult.FAILURE -> BsbUpdateStatus.BsbCheck.BsbCheckResult.FAILURE
        UpdateStatus.Check.CheckResult.NONE -> BsbUpdateStatus.BsbCheck.BsbCheckResult.NONE
    }
}

private fun UpdateStatus.Check.toBsbCheck(): BsbUpdateStatus.BsbCheck {
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
