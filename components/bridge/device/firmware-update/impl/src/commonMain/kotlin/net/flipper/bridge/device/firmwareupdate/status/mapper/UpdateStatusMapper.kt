package net.flipper.bridge.device.firmwareupdate.status.mapper

import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbCheck.BsbCheckResult
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall.BsbAction
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall.BsbStatus
import net.flipper.bridge.connection.feature.rpc.generated.model.UpdateStatus
import net.flipper.bridge.connection.feature.rpc.generated.model.UpdateStatusCheck
import net.flipper.bridge.connection.feature.rpc.generated.model.UpdateStatusInstall

@Suppress("CyclomaticComplexMethod")
internal fun UpdateStatus.toBsbUpdateStatus(): BsbUpdateStatus {
    return BsbUpdateStatus(
        install = BsbUpdateStatus.BsbInstall(
            isAllowed = install.isAllowed,
            action = when (install.action) {
                UpdateStatusInstall.Action.DOWNLOAD -> BsbAction.DOWNLOAD
                UpdateStatusInstall.Action.SHA_VERIFICATION -> BsbAction.SHA_VERIFICATION
                UpdateStatusInstall.Action.UNPACK -> BsbAction.UNPACK
                UpdateStatusInstall.Action.PREPARE -> BsbAction.PREPARE
                UpdateStatusInstall.Action.APPLY -> BsbAction.APPLY
                UpdateStatusInstall.Action.NONE -> BsbAction.NONE
            },
            status = when (install.status) {
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
            },
            download = BsbUpdateStatus.BsbInstall.BsbDownload(
                speedBytesPerSec = install.download.speedBytesPerSec,
                receivedBytes = install.download.receivedBytes,
                totalBytes = install.download.totalBytes
            )
        ),
        check = BsbUpdateStatus.BsbCheck(
            availableVersion = check.availableVersion,
            status = when (check.status) {
                UpdateStatusCheck.Status.AVAILABLE -> BsbCheckResult.AVAILABLE
                UpdateStatusCheck.Status.NOT_AVAILABLE -> BsbCheckResult.NOT_AVAILABLE
                UpdateStatusCheck.Status.FAILURE -> BsbCheckResult.FAILURE
                UpdateStatusCheck.Status.NONE -> BsbCheckResult.NONE
            }
        )
    )
}
