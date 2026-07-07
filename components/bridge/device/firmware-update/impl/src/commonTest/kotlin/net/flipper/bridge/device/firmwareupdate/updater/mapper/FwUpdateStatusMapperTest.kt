package net.flipper.bridge.device.firmwareupdate.updater.mapper

import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.connection.feature.firmwareupdate.model.DeviceUpdateStatus
import net.flipper.bridge.device.firmwareupdate.downloader.model.FirmwareDownloaderState
import net.flipper.bridge.device.firmwareupdate.status.model.UpdateStatusSource
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.bridge.device.firmwareupdate.uploader.model.FirmwareUploaderState
import kotlin.test.Test
import kotlin.test.assertEquals

class FwUpdateStatusMapperTest {

    @Test
    @Suppress("MaxLineLength")
    fun GIVEN_ready_status_and_no_update_version_and_pending_downloader_and_uploader_WHEN_map_THEN_returns_no_update_available() {
        val result = FwUpdateStatusMapper.map(
            updateStatusSource = UpdateStatusSource.Fresh(
                DeviceUpdateStatus("test-device-id", BsbUpdateStatus.ReadyToInstall.Ready)
            ),
            bsbUpdateVersion = BsbUpdateVersion.NoUpdateAvailable,
            downloaderState = FirmwareDownloaderState.Pending,
            uploaderState = FirmwareUploaderState.Pending,
            isInstallRequested = false
        )

        assertEquals(FwUpdateState.NoUpdateAvailable, result)
    }
}
