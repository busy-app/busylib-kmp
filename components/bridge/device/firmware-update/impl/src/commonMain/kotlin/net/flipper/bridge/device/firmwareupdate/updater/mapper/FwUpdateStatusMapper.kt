package net.flipper.bridge.device.firmwareupdate.updater.mapper

import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.device.firmwareupdate.downloader.model.FirmwareDownloaderState
import net.flipper.bridge.device.firmwareupdate.status.model.UpdateStatusSource
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.bridge.device.firmwareupdate.uploader.model.FirmwareUploaderState

internal object FwUpdateStatusMapper {

    private fun map(uploaderState: FirmwareUploaderState): FwUpdateState? {
        return when (uploaderState) {
            FirmwareUploaderState.Uploaded -> {
                FwUpdateState.Updating
            }

            is FirmwareUploaderState.Uploading -> {
                FwUpdateState.Uploading(uploaderState.progress)
            }

            FirmwareUploaderState.BatteryLow,
            FirmwareUploaderState.Pending,
            FirmwareUploaderState.Failed -> null
        }
    }

    private fun map(downloaderState: FirmwareDownloaderState): FwUpdateState.Downloading? {
        return when (downloaderState) {
            FirmwareDownloaderState.Downloaded -> {
                FwUpdateState.Downloading(1f, true)
            }

            is FirmwareDownloaderState.Downloading -> {
                FwUpdateState.Downloading(downloaderState.progress, true)
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

    private fun mapFreshUpdateStatusSource(
        updateStatusSource: UpdateStatusSource.Fresh,
        isLanUpdate: Boolean
    ): FwUpdateState? {
        return if (updateStatusSource.freshUpdateStatus == null) {
            FwUpdateState.Pending
        } else {
            when (updateStatusSource.freshUpdateStatus) {
                BsbUpdateStatus.InProgress.Downloading.NotSpecified -> {
                    FwUpdateState.Downloading(0f, isLanUpdate)
                }

                is BsbUpdateStatus.InProgress.Other -> FwUpdateState.Downloading(1f, isLanUpdate)
                is BsbUpdateStatus.InProgress.Downloading.Specified -> {
                    FwUpdateState.Downloading(
                        updateStatusSource.freshUpdateStatus.progress,
                        isLanUpdate
                    )
                }

                BsbUpdateStatus.ReadyToInstall.BatteryLow -> FwUpdateState.UpdateAvailable
                BsbUpdateStatus.ReadyToInstall.Ready -> FwUpdateState.UpdateAvailable
                BsbUpdateStatus.Loading -> null
            }
        }
    }

    private fun mapCachedUpdateStatusSource(
        updateStatusSource: UpdateStatusSource.Cached,
        isLanUpdate: Boolean
    ): FwUpdateState? {
        return when (updateStatusSource.cachedUpdateStatus) {
            BsbUpdateStatus.InProgress.Downloading.NotSpecified -> {
                FwUpdateState.Downloading(0f, isLanUpdate)
            }

            is BsbUpdateStatus.InProgress.Other -> FwUpdateState.Updating
            is BsbUpdateStatus.InProgress.Downloading.Specified -> {
                FwUpdateState.Downloading(
                    updateStatusSource.cachedUpdateStatus.progress,
                    isLanUpdate
                )
            }

            BsbUpdateStatus.ReadyToInstall.BatteryLow -> FwUpdateState.UpdateAvailable
            BsbUpdateStatus.ReadyToInstall.Ready -> FwUpdateState.UpdateAvailable
            BsbUpdateStatus.Loading -> null
        }
    }

    private fun map(
        updateStatusSource: UpdateStatusSource,
        isLanUpdate: Boolean
    ): FwUpdateState? {
        return when (updateStatusSource) {
            is UpdateStatusSource.Cached -> {
                mapCachedUpdateStatusSource(updateStatusSource, isLanUpdate)
            }

            is UpdateStatusSource.Fresh -> {
                mapFreshUpdateStatusSource(updateStatusSource, isLanUpdate)
            }
        }
    }

    fun map(
        updateStatusSource: UpdateStatusSource,
        bsbUpdateVersion: BsbUpdateVersion?,
        downloaderState: FirmwareDownloaderState,
        uploaderState: FirmwareUploaderState,
        isInstallRequested: Boolean
    ): FwUpdateState {
        val state = map(uploaderState)
            ?: map(downloaderState)
            ?: map(bsbUpdateVersion)
            ?: map(updateStatusSource, isLanUpdate(bsbUpdateVersion))
            ?: FwUpdateState.Pending
        return when (state) {
            FwUpdateState.UpdateAvailable if isInstallRequested -> FwUpdateState.Preparing
            is FwUpdateState.CheckingVersion,
            is FwUpdateState.CouldNotCheckUpdate,
            is FwUpdateState.DownloadFailure,
            is FwUpdateState.Downloading,
            is FwUpdateState.NoUpdateAvailable,
            is FwUpdateState.Pending,
            is FwUpdateState.Preparing,
            is FwUpdateState.Updating,
            is FwUpdateState.Uploading,
            is FwUpdateState.UpdateAvailable -> state
        }
    }

    private fun isLanUpdate(bsbUpdateVersion: BsbUpdateVersion?): Boolean {
        return when (bsbUpdateVersion) {
            is BsbUpdateVersion.ReadyToUpdate.Url -> true
            BsbUpdateVersion.CheckingOnBBInProgress,
            BsbUpdateVersion.FailedToCheck,
            BsbUpdateVersion.Loading,
            BsbUpdateVersion.NoUpdateAvailable,
            is BsbUpdateVersion.ReadyToUpdate.Default,
            null -> false
        }
    }
}
