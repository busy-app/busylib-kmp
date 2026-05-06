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
        availableVersion: String?
    ): FwUpdateState {
        return when(updateStatus) {
            is BsbUpdateStatus.FailedUpdate -> FwUpdateState.Failure
            BsbUpdateStatus.InProgress.Downloading.NotSpecified -> TODO()
            is BsbUpdateStatus.InProgress.Downloading.Specified -> TODO()
            is BsbUpdateStatus.InProgress.Other -> TODO()
            BsbUpdateStatus.ReadyToInstall.BatteryLow -> FwUpdateState.LowBattery
            BsbUpdateStatus.ReadyToInstall.Ready -> TODO()
            BsbUpdateStatus.InProgress.CheckingInProgress -> FwUpdateState.CheckingVersion
            BsbUpdateStatus.Loading -> TODO()
        }
    }
}
