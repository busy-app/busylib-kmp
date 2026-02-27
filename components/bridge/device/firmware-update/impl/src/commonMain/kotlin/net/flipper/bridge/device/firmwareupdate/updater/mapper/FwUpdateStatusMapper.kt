package net.flipper.bridge.device.firmwareupdate.updater.mapper

import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import net.flipper.bridge.device.firmwareupdate.downloader.model.FirmwareDownloaderState
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.bridge.device.firmwareupdate.uploader.model.FirmwareUploaderState

object FwUpdateStatusMapper {
    private fun fromCheckStatus(
        updateStatus: UpdateStatus,
    ): FwUpdateState {
        return when (updateStatus.check.status) {
            UpdateStatus.Check.CheckResult.AVAILABLE -> {
                FwUpdateState.UpdateAvailable
            }

            UpdateStatus.Check.CheckResult.NOT_AVAILABLE -> {
                FwUpdateState.NoUpdateAvailable
            }

            UpdateStatus.Check.CheckResult.FAILURE -> {
                FwUpdateState.CouldNotCheckUpdate
            }

            UpdateStatus.Check.CheckResult.NONE -> {
                FwUpdateState.CheckingVersion
            }
        }
    }

    private fun fromInstallAction(
        updateStatus: UpdateStatus,
    ): FwUpdateState {
        return when (updateStatus.install.action) {
            UpdateStatus.Install.Action.DOWNLOAD,
            UpdateStatus.Install.Action.SHA_VERIFICATION,
            UpdateStatus.Install.Action.UNPACK,
            UpdateStatus.Install.Action.APPLY,
            UpdateStatus.Install.Action.PREPARE -> {
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

            UpdateStatus.Install.Action.NONE -> {
                when (updateStatus.check.event) {
                    UpdateStatus.Check.CheckEvent.START,
                    UpdateStatus.Check.CheckEvent.NONE,
                    UpdateStatus.Check.CheckEvent.STOP -> {
                        fromCheckStatus(updateStatus = updateStatus,)
                    }
                }
            }
        }
    }

    private fun fromInstallStatus(
        updateStatus: UpdateStatus,
    ): FwUpdateState {
        return when (updateStatus.install.status) {
            UpdateStatus.Install.Status.BUSY,
            UpdateStatus.Install.Status.OK -> {
                fromInstallAction(updateStatus = updateStatus)
            }

            UpdateStatus.Install.Status.BATTERY_LOW -> FwUpdateState.LowBattery
            UpdateStatus.Install.Status.DOWNLOAD_ABORT,
            UpdateStatus.Install.Status.SHA_MISMATCH,
            UpdateStatus.Install.Status.UNPACK_STAGING_DIR_FAILURE,
            UpdateStatus.Install.Status.UNPACK_ARCHIVE_OPEN_FAILURE,
            UpdateStatus.Install.Status.UNPACK_ARCHIVE_UNPACK_FAILURE,
            UpdateStatus.Install.Status.INSTALL_MANIFEST_NOT_FOUND,
            UpdateStatus.Install.Status.INSTALL_MANIFEST_INVALID,
            UpdateStatus.Install.Status.INSTALL_SESSION_CONFIG_FAILURE,
            UpdateStatus.Install.Status.INSTALL_POINTER_SETUP_FAILURE,
            UpdateStatus.Install.Status.UNKNOWN_FAILURE,
            UpdateStatus.Install.Status.DOWNLOAD_FAILURE -> FwUpdateState.Failure
        }
    }

    fun toFwUpdateState(
        updateStatus: UpdateStatus?,
        uploaderState: FirmwareUploaderState,
        downloaderState: FirmwareDownloaderState,
        bsbUrlUpdateVersion: BsbUpdateVersion.Url?
    ): FwUpdateState {
        return when {
            uploaderState is FirmwareUploaderState.Uploading -> {
                FwUpdateState.Uploading(progress = uploaderState.progress)
            }

            uploaderState is FirmwareUploaderState.Uploaded -> {
                FwUpdateState.Uploading(progress = 1f)
            }

            downloaderState is FirmwareDownloaderState.Downloading -> {
                FwUpdateState.Downloading(progress = downloaderState.progress)
            }

            downloaderState is FirmwareDownloaderState.Downloaded -> {
                FwUpdateState.Downloading(progress = 1f)
            }

            updateStatus == null -> {
                FwUpdateState.Pending
            }

            bsbUrlUpdateVersion != null -> {
                FwUpdateState.UpdateAvailable
            }

            else -> {
                fromInstallStatus(
                    updateStatus = updateStatus,
                )
            }
        }
    }
}
