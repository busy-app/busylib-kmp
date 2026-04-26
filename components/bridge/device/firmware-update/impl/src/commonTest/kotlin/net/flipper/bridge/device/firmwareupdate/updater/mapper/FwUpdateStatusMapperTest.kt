package net.flipper.bridge.device.firmwareupdate.updater.mapper

import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbCheck
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbCheck.BsbCheckResult
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall.BsbAction
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall.BsbDownload
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall.BsbStatus
import net.flipper.bridge.device.firmwareupdate.status.model.UpdateStatusSource
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import kotlin.test.Test
import kotlin.test.assertEquals

class FwUpdateStatusMapperTest {

    @Test
    fun GIVEN_user_aborted_download_with_check_available_WHEN_map_THEN_update_available() {
        val abortedAfterCheckAvailable = makeStatus(
            installStatus = BsbStatus.DOWNLOAD_ABORT,
            installAction = BsbAction.DOWNLOAD,
            checkResult = BsbCheckResult.AVAILABLE
        )

        val state = FwUpdateStatusMapper.toFwUpdateState(UpdateStatusSource.Fresh(abortedAfterCheckAvailable))

        assertEquals(FwUpdateState.UpdateAvailable, state)
    }

    @Test
    fun GIVEN_user_aborted_download_with_no_update_available_WHEN_map_THEN_no_update_available() {
        val abortedNoUpdate = makeStatus(
            installStatus = BsbStatus.DOWNLOAD_ABORT,
            installAction = BsbAction.DOWNLOAD,
            checkResult = BsbCheckResult.NOT_AVAILABLE
        )

        val state = FwUpdateStatusMapper.toFwUpdateState(UpdateStatusSource.Fresh(abortedNoUpdate))

        assertEquals(FwUpdateState.NoUpdateAvailable, state)
    }

    @Test
    fun GIVEN_real_download_failure_WHEN_map_THEN_download_failure_unchanged() {
        val downloadFailed = makeStatus(
            installStatus = BsbStatus.DOWNLOAD_FAILURE,
            installAction = BsbAction.DOWNLOAD,
            checkResult = BsbCheckResult.AVAILABLE
        )

        val state = FwUpdateStatusMapper.toFwUpdateState(UpdateStatusSource.Fresh(downloadFailed))

        assertEquals(FwUpdateState.DownloadFailure, state)
    }

    private fun makeStatus(
        installStatus: BsbStatus,
        installAction: BsbAction,
        checkResult: BsbCheckResult
    ): BsbUpdateStatus = BsbUpdateStatus(
        install = BsbInstall(
            isAllowed = true,
            action = installAction,
            status = installStatus,
            download = BsbDownload(speedBytesPerSec = 0, receivedBytes = 0, totalBytes = 0)
        ),
        check = BsbCheck(availableVersion = "44ee94a8", status = checkResult)
    )
}
