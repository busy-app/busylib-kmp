package net.flipper.bridge.device.firmwareupdate.updater.mapper

import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.device.firmwareupdate.downloader.model.FirmwareDownloaderState
import net.flipper.bridge.device.firmwareupdate.status.model.UpdateStatusSource
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.bridge.device.firmwareupdate.uploader.model.FirmwareUploaderState

internal object FwUpdateStatusMapper {
    fun from(
        updateStatus: BsbUpdateStatus,
        availableVersion: BsbUpdateVersion,
        uploaderState: FirmwareUploaderState,
        downloaderState: FirmwareDownloaderState,
    ): FwUpdateState {
        when(uploaderState) {
            FirmwareUploaderState.Failed,
            FirmwareUploaderState.Pending -> TODO()
            FirmwareUploaderState.Uploaded -> TODO()
            is FirmwareUploaderState.Uploading -> TODO()
        }
        return when (availableVersion) {
            BsbUpdateVersion.CheckingOnBBInProgress -> FwUpdateState.CheckingVersion
            BsbUpdateVersion.FailedToCheck -> FwUpdateState.CouldNotCheckUpdate
            BsbUpdateVersion.Loading -> FwUpdateState.Pending
            BsbUpdateVersion.NoUpdateAvailable -> FwUpdateState.NoUpdateAvailable
            is BsbUpdateVersion.ReadyToUpdate.Default -> TODO()
            is BsbUpdateVersion.ReadyToUpdate.Url -> TODO()
        }
    }

    private fun from(
        updateStatus: BsbUpdateStatus,
        uploaderState: FirmwareUploaderState,
        downloaderState: FirmwareDownloaderState
    ) {
        when (uploaderState) {
            FirmwareUploaderState.Failed -> TODO()
            FirmwareUploaderState.Pending -> TODO()
            FirmwareUploaderState.Uploaded -> TODO()
            is FirmwareUploaderState.Uploading -> TODO()
        }
        when (updateStatus) {
            is BsbUpdateStatus.FailedUpdate -> FwUpdateState.Failure
            BsbUpdateStatus.InProgress.Downloading.NotSpecified -> TODO()
            is BsbUpdateStatus.InProgress.Downloading.Specified -> TODO()
            is BsbUpdateStatus.InProgress.Other -> FwUpdateState.Updating
            BsbUpdateStatus.ReadyToInstall.BatteryLow -> FwUpdateState.LowBattery
            BsbUpdateStatus.ReadyToInstall.Ready -> TODO()
            BsbUpdateStatus.Loading -> FwUpdateState.Pending
        }
    }
}
