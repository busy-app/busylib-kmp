package net.flipper.bridge.device.firmwareupdate.updater.detector

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbCheck
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbCheck.BsbCheckResult
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall.BsbAction
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall.BsbDownload
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall.BsbStatus
import net.flipper.bridge.device.firmwareupdate.status.model.UpdateStatusSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InstallCompletionDetectorTest {

    @Test
    fun GIVEN_install_active_then_idle_WHEN_detect_THEN_emits_once() = runTest {
        val statuses = flowOf(
            UpdateStatusSource.Fresh(makeStatus(action = BsbAction.APPLY, status = BsbStatus.OK)),
            UpdateStatusSource.Fresh(makeStatus(action = BsbAction.NONE, status = BsbStatus.OK)),
        )

        val emissions = InstallCompletionDetector.detect(statuses).toList()

        assertEquals(listOf(Unit), emissions)
    }

    @Test
    fun GIVEN_only_idle_observed_WHEN_detect_THEN_no_emission() = runTest {
        val statuses = flowOf(
            UpdateStatusSource.Fresh(makeStatus(action = BsbAction.NONE, status = BsbStatus.OK)),
            UpdateStatusSource.Fresh(makeStatus(action = BsbAction.NONE, status = BsbStatus.OK)),
        )

        val emissions = InstallCompletionDetector.detect(statuses).toList()

        assertTrue(emissions.isEmpty(), "Initial idle without prior active must not signal completion")
    }

    @Test
    fun GIVEN_lan_to_cloud_handoff_with_status_gap_WHEN_detect_THEN_emits_on_post_reboot_idle() = runTest {
        val statuses = flowOf(
            // LAN-side polls during apply
            UpdateStatusSource.Fresh(makeStatus(action = BsbAction.DOWNLOAD, status = BsbStatus.OK)),
            UpdateStatusSource.Fresh(makeStatus(action = BsbAction.UNPACK, status = BsbStatus.OK)),
            UpdateStatusSource.Fresh(makeStatus(action = BsbAction.APPLY, status = BsbStatus.OK)),
            // Cloud poll after reboot — device reports idle
            UpdateStatusSource.Fresh(
                makeStatus(action = BsbAction.NONE, status = BsbStatus.OK, receivedBytes = 0)
            ),
        )

        val emissions = InstallCompletionDetector.detect(statuses).toList()

        assertEquals(listOf(Unit), emissions)
    }

    @Test
    fun GIVEN_active_then_idle_with_failure_status_WHEN_detect_THEN_no_emission() = runTest {
        val statuses = flowOf(
            UpdateStatusSource.Fresh(makeStatus(action = BsbAction.APPLY, status = BsbStatus.OK)),
            UpdateStatusSource.Fresh(makeStatus(action = BsbAction.NONE, status = BsbStatus.UNKNOWN_FAILURE)),
        )

        val emissions = InstallCompletionDetector.detect(statuses).toList()

        assertTrue(emissions.isEmpty(), "Idle with non-OK status must not be treated as completion")
    }

    @Test
    fun GIVEN_null_fresh_status_WHEN_detect_THEN_no_emission() = runTest {
        val statuses = flowOf(
            UpdateStatusSource.Fresh(freshUpdateStatus = null),
            UpdateStatusSource.Fresh(freshUpdateStatus = null),
        )

        val emissions = InstallCompletionDetector.detect(statuses).toList()

        assertTrue(emissions.isEmpty())
    }

    @Test
    fun GIVEN_active_then_active_again_WHEN_detect_THEN_no_emission_until_idle() = runTest {
        val statuses = flowOf(
            UpdateStatusSource.Fresh(makeStatus(action = BsbAction.DOWNLOAD, status = BsbStatus.OK)),
            UpdateStatusSource.Fresh(makeStatus(action = BsbAction.APPLY, status = BsbStatus.OK)),
            UpdateStatusSource.Fresh(makeStatus(action = BsbAction.APPLY, status = BsbStatus.BUSY)),
        )

        val emissions = InstallCompletionDetector.detect(statuses).toList()

        assertTrue(emissions.isEmpty())
    }

    private fun makeStatus(
        action: BsbAction,
        status: BsbStatus,
        receivedBytes: Int = 0,
    ): BsbUpdateStatus = BsbUpdateStatus(
        install = BsbInstall(
            isAllowed = true,
            action = action,
            status = status,
            download = BsbDownload(speedBytesPerSec = 0, receivedBytes = receivedBytes, totalBytes = 0)
        ),
        check = BsbCheck(availableVersion = "", status = BsbCheckResult.NONE)
    )
}
