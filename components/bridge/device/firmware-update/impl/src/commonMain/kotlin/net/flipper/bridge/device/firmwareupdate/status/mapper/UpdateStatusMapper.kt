package net.flipper.bridge.device.firmwareupdate.status.mapper

import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbCheck.BsbCheckResult
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall.BsbAction
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall.BsbStatus
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus.Check.CheckResult
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus.Install.Action
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus.Install.Status

@Suppress("CyclomaticComplexMethod")
internal fun UpdateStatus.toBsbUpdateStatus(): BsbUpdateStatus {
    return BsbUpdateStatus(
        install = BsbUpdateStatus.BsbInstall(
            isAllowed = install.isAllowed,
            action = when (install.action) {
                Action.DOWNLOAD -> BsbAction.DOWNLOAD
                Action.SHA_VERIFICATION -> BsbAction.SHA_VERIFICATION
                Action.UNPACK -> BsbAction.UNPACK
                Action.PREPARE -> BsbAction.PREPARE
                Action.APPLY -> BsbAction.APPLY
                Action.NONE -> BsbAction.NONE
            },
            status = when (install.status) {
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
                CheckResult.AVAILABLE -> BsbCheckResult.AVAILABLE
                CheckResult.NOT_AVAILABLE -> BsbCheckResult.NOT_AVAILABLE
                CheckResult.FAILURE -> BsbCheckResult.FAILURE
                CheckResult.NONE -> BsbCheckResult.NONE
            }
        )
    )
}
