package net.flipper.bridge.device.firmwareupdate.updater.mapper

import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.device.firmwareupdate.downloader.model.FirmwareDownloaderState
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.bridge.device.firmwareupdate.uploader.model.FirmwareUploaderState

internal object FwUpdateStatusMapper {

    private fun map(uploaderState: FirmwareUploaderState): FwUpdateState.Uploading? {
        return when (uploaderState) {
            FirmwareUploaderState.Uploaded -> {
                FwUpdateState.Uploading(1f)
            }

            is FirmwareUploaderState.Uploading -> {
                FwUpdateState.Uploading(uploaderState.progress)
            }

            FirmwareUploaderState.Pending,
            FirmwareUploaderState.Failed -> null
        }
    }

    private fun map(downloaderState: FirmwareDownloaderState): FwUpdateState.Downloading? {
        return when (downloaderState) {
            FirmwareDownloaderState.Downloaded -> {
                FwUpdateState.Downloading(1f)
            }

            is FirmwareDownloaderState.Downloading -> {
                FwUpdateState.Downloading(downloaderState.progress)
            }

            FirmwareDownloaderState.Pending -> null
        }
    }

    private fun map(bsbUpdateVersion: BsbUpdateVersion?): FwUpdateState? {
        return when (bsbUpdateVersion) {
            BsbUpdateVersion.CheckingOnBBInProgress -> FwUpdateState.CheckingVersion
            BsbUpdateVersion.FailedToCheck -> FwUpdateState.CouldNotCheckUpdate
            BsbUpdateVersion.Loading -> FwUpdateState.Pending
            BsbUpdateVersion.NoUpdateAvailable -> FwUpdateState.NoUpdateAvailable
            // Download by application
            is BsbUpdateVersion.ReadyToUpdate.Url -> FwUpdateState.UpdateAvailable
            // Download by device itself
            is BsbUpdateVersion.ReadyToUpdate.Default,
            null -> null
        }
    }

    val BsbUpdateStatus.InProgress.Downloading.Specified.progress: Float
        get() = when {
            totalBytes <= 0 -> 0f
            else -> receivedBytes.toFloat()
                .div(totalBytes)
                .coerceIn(0f, 1f)
        }

    private fun map(bsbUpdateStatus: BsbUpdateStatus): FwUpdateState? {
        return when (bsbUpdateStatus) {
            BsbUpdateStatus.InProgress.Downloading.NotSpecified -> FwUpdateState.Downloading(0f)
            is BsbUpdateStatus.InProgress.Other -> FwUpdateState.Downloading(1f)
            is BsbUpdateStatus.InProgress.Downloading.Specified -> {
                FwUpdateState.Downloading(bsbUpdateStatus.progress)
            }

            BsbUpdateStatus.ReadyToInstall.BatteryLow -> FwUpdateState.LowBattery
            BsbUpdateStatus.ReadyToInstall.Ready -> FwUpdateState.UpdateAvailable
            BsbUpdateStatus.Loading -> null
        }
    }

    fun map(
        bsbUpdateStatus: BsbUpdateStatus,
        bsbUpdateVersion: BsbUpdateVersion?,
        downloaderState: FirmwareDownloaderState,
        uploaderState: FirmwareUploaderState
    ): FwUpdateState {
        return map(uploaderState)
            ?: map(downloaderState)
            ?: map(bsbUpdateVersion)
            ?: map(bsbUpdateStatus)
            ?: FwUpdateState.Pending
    }
}
