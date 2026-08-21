package net.flipper.bridge.device.firmwareupdate.updater.mapper

import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.connection.feature.firmwareupdate.model.FirmwareChannel
import net.flipper.bridge.device.firmwareupdate.downloader.model.FirmwareDownloaderState
import net.flipper.bridge.device.firmwareupdate.status.model.UpdateStatusSource
import net.flipper.bridge.device.firmwareupdate.updater.model.FwInstallRequest
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.bridge.device.firmwareupdate.uploader.model.FirmwareUploaderState
import kotlin.test.Test
import kotlin.test.assertEquals

class FwUpdateStatusMapperTest {

    @Test
    @Suppress("MaxLineLength")
    fun GIVEN_ready_status_and_no_update_version_and_pending_downloader_and_uploader_WHEN_map_THEN_returns_no_update_available() {
        val result = FwUpdateStatusMapper.map(
            updateStatusSource = UpdateStatusSource.Fresh(BsbUpdateStatus.ReadyToInstall.Ready),
            bsbUpdateVersion = BsbUpdateVersion.NoUpdateAvailable,
            downloaderState = FirmwareDownloaderState.Pending,
            uploaderState = FirmwareUploaderState.Pending,
            installRequest = FwInstallRequest.NONE
        )

        assertEquals(FwUpdateState.NoUpdateAvailable, result)
    }

    @Test
    fun GIVEN_ready_status_and_update_available_WHEN_map_THEN_returns_update_available() {
        val result = FwUpdateStatusMapper.map(
            updateStatusSource = UpdateStatusSource.Fresh(BsbUpdateStatus.ReadyToInstall.Ready),
            bsbUpdateVersion = BsbUpdateVersion.ReadyToUpdate.Default(UPDATE_VERSION),
            downloaderState = FirmwareDownloaderState.Pending,
            uploaderState = FirmwareUploaderState.Pending,
            installRequest = FwInstallRequest.NONE
        )

        assertEquals(FwUpdateState.UpdateAvailable, result)
    }

    @Test
    fun GIVEN_install_not_allowed_and_update_available_WHEN_map_THEN_returns_battery_low() {
        val result = FwUpdateStatusMapper.map(
            updateStatusSource = UpdateStatusSource.Fresh(BsbUpdateStatus.ReadyToInstall.BatteryLow),
            bsbUpdateVersion = BsbUpdateVersion.ReadyToUpdate.Default(UPDATE_VERSION),
            downloaderState = FirmwareDownloaderState.Pending,
            uploaderState = FirmwareUploaderState.Pending,
            installRequest = FwInstallRequest.NONE
        )

        assertEquals(FwUpdateState.BatteryLow, result)
    }

    @Test
    fun GIVEN_cached_install_not_allowed_and_update_available_WHEN_map_THEN_returns_battery_low() {
        val result = FwUpdateStatusMapper.map(
            updateStatusSource = UpdateStatusSource.Cached(BsbUpdateStatus.ReadyToInstall.BatteryLow),
            bsbUpdateVersion = BsbUpdateVersion.ReadyToUpdate.Default(UPDATE_VERSION),
            downloaderState = FirmwareDownloaderState.Pending,
            uploaderState = FirmwareUploaderState.Pending,
            installRequest = FwInstallRequest.NONE
        )

        assertEquals(FwUpdateState.BatteryLow, result)
    }

    @Test
    fun GIVEN_update_available_and_install_requested_WHEN_map_THEN_returns_preparing() {
        val result = FwUpdateStatusMapper.map(
            updateStatusSource = UpdateStatusSource.Fresh(BsbUpdateStatus.ReadyToInstall.Ready),
            bsbUpdateVersion = BsbUpdateVersion.ReadyToUpdate.Default(UPDATE_VERSION),
            downloaderState = FirmwareDownloaderState.Pending,
            uploaderState = FirmwareUploaderState.Pending,
            installRequest = FwInstallRequest.REQUESTED
        )

        assertEquals(FwUpdateState.Preparing, result)
    }

    @Test
    fun GIVEN_install_not_allowed_and_install_requested_WHEN_map_THEN_stays_battery_low() {
        val result = FwUpdateStatusMapper.map(
            updateStatusSource = UpdateStatusSource.Fresh(BsbUpdateStatus.ReadyToInstall.BatteryLow),
            bsbUpdateVersion = BsbUpdateVersion.ReadyToUpdate.Default(UPDATE_VERSION),
            downloaderState = FirmwareDownloaderState.Pending,
            uploaderState = FirmwareUploaderState.Pending,
            installRequest = FwInstallRequest.REQUESTED
        )

        assertEquals(FwUpdateState.BatteryLow, result)
    }

    @Test
    fun GIVEN_device_reports_download_progress_WHEN_map_THEN_returns_downloading_with_progress() {
        val result = FwUpdateStatusMapper.map(
            updateStatusSource = UpdateStatusSource.Fresh(
                BsbUpdateStatus.InProgress.Downloading.Specified(
                    speedBytesPerSec = 0,
                    receivedBytes = 512,
                    totalBytes = 1024
                )
            ),
            bsbUpdateVersion = BsbUpdateVersion.ReadyToUpdate.Default(UPDATE_VERSION),
            downloaderState = FirmwareDownloaderState.Pending,
            uploaderState = FirmwareUploaderState.Pending,
            installRequest = FwInstallRequest.REQUESTED
        )

        assertEquals(FwUpdateState.Downloading(progress = 0.5f, isLanUpdate = false), result)
    }

    @Test
    fun GIVEN_unknown_status_and_no_install_requested_WHEN_map_THEN_returns_pending() {
        val result = FwUpdateStatusMapper.map(
            updateStatusSource = UpdateStatusSource.Fresh(null),
            bsbUpdateVersion = BsbUpdateVersion.ReadyToUpdate.Default(UPDATE_VERSION),
            downloaderState = FirmwareDownloaderState.Pending,
            uploaderState = FirmwareUploaderState.Pending,
            installRequest = FwInstallRequest.NONE
        )

        assertEquals(FwUpdateState.Pending, result)
    }

    @Test
    fun GIVEN_unknown_status_and_install_requested_WHEN_map_THEN_returns_preparing() {
        // An unknown status must not hide a request the user just made
        val result = FwUpdateStatusMapper.map(
            updateStatusSource = UpdateStatusSource.Fresh(null),
            bsbUpdateVersion = BsbUpdateVersion.ReadyToUpdate.Default(UPDATE_VERSION),
            downloaderState = FirmwareDownloaderState.Pending,
            uploaderState = FirmwareUploaderState.Pending,
            installRequest = FwInstallRequest.REQUESTED
        )

        assertEquals(FwUpdateState.Preparing, result)
    }

    @Test
    fun GIVEN_unknown_status_and_install_started_WHEN_map_THEN_returns_downloading() {
        val result = FwUpdateStatusMapper.map(
            updateStatusSource = UpdateStatusSource.Fresh(null),
            bsbUpdateVersion = BsbUpdateVersion.ReadyToUpdate.Default(UPDATE_VERSION),
            downloaderState = FirmwareDownloaderState.Pending,
            uploaderState = FirmwareUploaderState.Pending,
            installRequest = FwInstallRequest.STARTED
        )

        assertEquals(FwUpdateState.Downloading(progress = 0f, isLanUpdate = false), result)
    }

    @Test
    fun GIVEN_install_started_and_device_reports_nothing_WHEN_map_THEN_returns_downloading() {
        // BSB accepted the install but never streams progress outside of BLE, the update must
        // still be visible instead of falling back to "update available"
        val result = FwUpdateStatusMapper.map(
            updateStatusSource = UpdateStatusSource.Fresh(BsbUpdateStatus.ReadyToInstall.Ready),
            bsbUpdateVersion = BsbUpdateVersion.ReadyToUpdate.Default(UPDATE_VERSION),
            downloaderState = FirmwareDownloaderState.Pending,
            uploaderState = FirmwareUploaderState.Pending,
            installRequest = FwInstallRequest.STARTED
        )

        assertEquals(FwUpdateState.Downloading(progress = 0f, isLanUpdate = false), result)
    }

    @Test
    fun GIVEN_install_started_and_device_reports_progress_WHEN_map_THEN_device_progress_wins() {
        val result = FwUpdateStatusMapper.map(
            updateStatusSource = UpdateStatusSource.Fresh(
                BsbUpdateStatus.InProgress.Downloading.Specified(
                    speedBytesPerSec = 0,
                    receivedBytes = 256,
                    totalBytes = 1024
                )
            ),
            bsbUpdateVersion = BsbUpdateVersion.ReadyToUpdate.Default(UPDATE_VERSION),
            downloaderState = FirmwareDownloaderState.Pending,
            uploaderState = FirmwareUploaderState.Pending,
            installRequest = FwInstallRequest.STARTED
        )

        assertEquals(FwUpdateState.Downloading(progress = 0.25f, isLanUpdate = false), result)
    }

    @Test
    fun GIVEN_lan_url_version_and_ready_status_WHEN_map_THEN_returns_update_available() {
        val result = FwUpdateStatusMapper.map(
            updateStatusSource = UpdateStatusSource.Fresh(BsbUpdateStatus.ReadyToInstall.Ready),
            bsbUpdateVersion = LAN_UPDATE_VERSION,
            downloaderState = FirmwareDownloaderState.Pending,
            uploaderState = FirmwareUploaderState.Pending,
            installRequest = FwInstallRequest.NONE
        )

        assertEquals(FwUpdateState.UpdateAvailable, result)
    }

    @Test
    fun GIVEN_lan_url_version_and_battery_low_status_WHEN_map_THEN_returns_battery_low() {
        // Regression: Url used to map to UpdateAvailable and shadow the device battery status
        val result = FwUpdateStatusMapper.map(
            updateStatusSource = UpdateStatusSource.Fresh(BsbUpdateStatus.ReadyToInstall.BatteryLow),
            bsbUpdateVersion = LAN_UPDATE_VERSION,
            downloaderState = FirmwareDownloaderState.Pending,
            uploaderState = FirmwareUploaderState.Pending,
            installRequest = FwInstallRequest.NONE
        )

        assertEquals(FwUpdateState.BatteryLow, result)
    }

    @Test
    fun GIVEN_lan_url_version_and_cached_battery_low_status_WHEN_map_THEN_returns_battery_low() {
        val result = FwUpdateStatusMapper.map(
            updateStatusSource = UpdateStatusSource.Cached(BsbUpdateStatus.ReadyToInstall.BatteryLow),
            bsbUpdateVersion = LAN_UPDATE_VERSION,
            downloaderState = FirmwareDownloaderState.Pending,
            uploaderState = FirmwareUploaderState.Pending,
            installRequest = FwInstallRequest.NONE
        )

        assertEquals(FwUpdateState.BatteryLow, result)
    }

    @Test
    fun GIVEN_version_loading_and_battery_low_status_WHEN_map_THEN_returns_battery_low() {
        // LAN version resolution takes seconds; battery low must not be hidden meanwhile
        val result = FwUpdateStatusMapper.map(
            updateStatusSource = UpdateStatusSource.Fresh(BsbUpdateStatus.ReadyToInstall.BatteryLow),
            bsbUpdateVersion = BsbUpdateVersion.Loading,
            downloaderState = FirmwareDownloaderState.Pending,
            uploaderState = FirmwareUploaderState.Pending,
            installRequest = FwInstallRequest.NONE
        )

        assertEquals(FwUpdateState.BatteryLow, result)
    }

    @Test
    fun GIVEN_no_update_available_and_battery_low_status_WHEN_map_THEN_returns_no_update_available() {
        // Without an update to install the battery level is irrelevant
        val result = FwUpdateStatusMapper.map(
            updateStatusSource = UpdateStatusSource.Fresh(BsbUpdateStatus.ReadyToInstall.BatteryLow),
            bsbUpdateVersion = BsbUpdateVersion.NoUpdateAvailable,
            downloaderState = FirmwareDownloaderState.Pending,
            uploaderState = FirmwareUploaderState.Pending,
            installRequest = FwInstallRequest.NONE
        )

        assertEquals(FwUpdateState.NoUpdateAvailable, result)
    }

    @Test
    fun GIVEN_lan_url_version_and_uploader_battery_low_WHEN_map_THEN_returns_battery_low() {
        // Device refused the install after the upload finished
        val result = FwUpdateStatusMapper.map(
            updateStatusSource = UpdateStatusSource.Fresh(BsbUpdateStatus.ReadyToInstall.Ready),
            bsbUpdateVersion = LAN_UPDATE_VERSION,
            downloaderState = FirmwareDownloaderState.Pending,
            uploaderState = FirmwareUploaderState.BatteryLow,
            installRequest = FwInstallRequest.NONE
        )

        assertEquals(FwUpdateState.BatteryLow, result)
    }

    private companion object {
        private const val UPDATE_VERSION = "1.1.1"
        private val LAN_UPDATE_VERSION = BsbUpdateVersion.ReadyToUpdate.Url(
            version = UPDATE_VERSION,
            url = "https://update.example/busybar-f21-update_signed-1.1.1.tgz",
            sha256 = "2f0b654001ebb0a3d83b4a1c0e55d88fc0c5ee39f043f2771353e41b36302d19",
            changelog = "changelog",
            firmwareChannel = FirmwareChannel.RELEASE
        )
    }
}
