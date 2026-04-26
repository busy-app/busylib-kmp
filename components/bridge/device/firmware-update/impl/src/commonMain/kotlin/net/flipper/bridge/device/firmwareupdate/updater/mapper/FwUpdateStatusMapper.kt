package net.flipper.bridge.device.firmwareupdate.updater.mapper

import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.device.firmwareupdate.downloader.model.FirmwareDownloaderState
import net.flipper.bridge.device.firmwareupdate.status.model.UpdateStatusSource
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.bridge.device.firmwareupdate.uploader.model.FirmwareUploaderState

internal object FwUpdateStatusMapper {
    private fun fromCheckStatus(
        updateStatus: BsbUpdateStatus,
    ): FwUpdateState {
        return when (updateStatus.check.status) {
            BsbUpdateStatus.BsbCheck.BsbCheckResult.AVAILABLE -> {
                FwUpdateState.UpdateAvailable
            }

            BsbUpdateStatus.BsbCheck.BsbCheckResult.NOT_AVAILABLE -> {
                FwUpdateState.NoUpdateAvailable
            }

            BsbUpdateStatus.BsbCheck.BsbCheckResult.FAILURE -> {
                FwUpdateState.CouldNotCheckUpdate
            }

            BsbUpdateStatus.BsbCheck.BsbCheckResult.NONE -> {
                FwUpdateState.CheckingVersion
            }
        }
    }

    private fun fromInstallAction(
        updateStatus: BsbUpdateStatus,
    ): FwUpdateState {
        return when (updateStatus.install.action) {
            BsbUpdateStatus.BsbInstall.BsbAction.DOWNLOAD,
            BsbUpdateStatus.BsbInstall.BsbAction.SHA_VERIFICATION,
            BsbUpdateStatus.BsbInstall.BsbAction.UNPACK,
            BsbUpdateStatus.BsbInstall.BsbAction.APPLY,
            BsbUpdateStatus.BsbInstall.BsbAction.PREPARE -> {
                FwUpdateState.Downloading(
                    progress = updateStatus.install.download.totalBytes
                        .toFloat()
                        .takeIf { total -> total > 0 }
                        ?.let { total ->
                            updateStatus.install
                                .download
                                .receivedBytes
                                .div(total)
                        }
                        ?: 0f
                )
            }

            BsbUpdateStatus.BsbInstall.BsbAction.NONE -> {
                fromCheckStatus(updateStatus = updateStatus)
            }
        }
    }

    private fun fromInstallStatus(
        updateStatus: BsbUpdateStatus,
    ): FwUpdateState {
        return when (updateStatus.install.status) {
            BsbUpdateStatus.BsbInstall.BsbStatus.BUSY,
            BsbUpdateStatus.BsbInstall.BsbStatus.OK -> {
                fromInstallAction(updateStatus = updateStatus)
            }

            BsbUpdateStatus.BsbInstall.BsbStatus.DOWNLOAD_FAILURE -> FwUpdateState.DownloadFailure
            BsbUpdateStatus.BsbInstall.BsbStatus.DOWNLOAD_ABORT -> fromCheckStatus(updateStatus = updateStatus)
            BsbUpdateStatus.BsbInstall.BsbStatus.BATTERY_LOW -> FwUpdateState.LowBattery
            BsbUpdateStatus.BsbInstall.BsbStatus.SHA_MISMATCH,
            BsbUpdateStatus.BsbInstall.BsbStatus.UNPACK_STAGING_DIR_FAILURE,
            BsbUpdateStatus.BsbInstall.BsbStatus.UNPACK_ARCHIVE_OPEN_FAILURE,
            BsbUpdateStatus.BsbInstall.BsbStatus.UNPACK_ARCHIVE_UNPACK_FAILURE,
            BsbUpdateStatus.BsbInstall.BsbStatus.INSTALL_MANIFEST_NOT_FOUND,
            BsbUpdateStatus.BsbInstall.BsbStatus.INSTALL_MANIFEST_INVALID,
            BsbUpdateStatus.BsbInstall.BsbStatus.INSTALL_SESSION_CONFIG_FAILURE,
            BsbUpdateStatus.BsbInstall.BsbStatus.INSTALL_POINTER_SETUP_FAILURE,
            BsbUpdateStatus.BsbInstall.BsbStatus.UNKNOWN_FAILURE -> FwUpdateState.Failure
        }
    }

    fun toFwUpdateState(
        uploaderState: FirmwareUploaderState,
        downloaderState: FirmwareDownloaderState,
        bsbUrlUpdateVersion: BsbUpdateVersion.Url?,
    ): FwUpdateState {
        return when {
            uploaderState is FirmwareUploaderState.Uploading -> {
                FwUpdateState.Uploading(progress = uploaderState.progress)
            }

            uploaderState is FirmwareUploaderState.Uploaded -> {
                FwUpdateState.Updating
            }

            downloaderState is FirmwareDownloaderState.Downloading -> {
                FwUpdateState.Downloading(progress = downloaderState.progress)
            }

            downloaderState is FirmwareDownloaderState.Downloaded -> {
                FwUpdateState.Downloading(progress = 1f)
            }

            bsbUrlUpdateVersion != null -> {
                FwUpdateState.UpdateAvailable
            }

            else -> {
                FwUpdateState.Pending
            }
        }
    }

    fun toFwUpdateState(updateStatusSource: UpdateStatusSource): FwUpdateState {
        return when (updateStatusSource) {
            is UpdateStatusSource.Cached -> {
                if (updateStatusSource.freshUpdateStatus == null) {
                    when (updateStatusSource.cachedUpdateStatus.install.action) {
                        BsbUpdateStatus.BsbInstall.BsbAction.UNPACK,
                        BsbUpdateStatus.BsbInstall.BsbAction.SHA_VERIFICATION,
                        BsbUpdateStatus.BsbInstall.BsbAction.DOWNLOAD -> FwUpdateState.Updating

                        BsbUpdateStatus.BsbInstall.BsbAction.PREPARE,
                        BsbUpdateStatus.BsbInstall.BsbAction.APPLY,
                        BsbUpdateStatus.BsbInstall.BsbAction.NONE -> FwUpdateState.Pending
                    }
                } else {
                    fromInstallStatus(updateStatus = updateStatusSource.freshUpdateStatus)
                }
            }

            is UpdateStatusSource.Fresh -> {
                if (updateStatusSource.freshUpdateStatus == null) {
                    FwUpdateState.Pending
                } else {
                    fromInstallStatus(updateStatus = updateStatusSource.freshUpdateStatus)
                }
            }
        }
    }
}
