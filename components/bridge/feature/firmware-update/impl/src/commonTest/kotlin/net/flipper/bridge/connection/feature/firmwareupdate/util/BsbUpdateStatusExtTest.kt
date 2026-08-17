package net.flipper.bridge.connection.feature.firmwareupdate.util

import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class BsbUpdateStatusExtTest {

    @Suppress("LongParameterList")
    private fun createUpdateStatus(
        isAllowed: Boolean,
        event: UpdateStatus.Install.Event = UpdateStatus.Install.Event.NONE,
        action: UpdateStatus.Install.Action = UpdateStatus.Install.Action.NONE,
        status: UpdateStatus.Install.Status = UpdateStatus.Install.Status.OK,
        receivedBytes: Int = 0,
        totalBytes: Int = 0
    ): UpdateStatus {
        return UpdateStatus(
            install = UpdateStatus.Install(
                isAllowed = isAllowed,
                event = event,
                action = action,
                status = status,
                detail = "",
                download = UpdateStatus.Install.Download(
                    speedBytesPerSec = 0,
                    receivedBytes = receivedBytes,
                    totalBytes = totalBytes
                )
            ),
            check = UpdateStatus.Check(
                availableVersion = "",
                event = UpdateStatus.Check.CheckEvent.NONE,
                status = UpdateStatus.Check.CheckResult.NONE
            )
        )
    }

    @Test
    fun GIVEN_install_allowed_WHEN_mapped_THEN_returns_ready() {
        val result = createUpdateStatus(isAllowed = true).toBsbUpdateStatus()

        assertEquals(BsbUpdateStatus.ReadyToInstall.Ready, result)
    }

    @Test
    fun GIVEN_install_not_allowed_and_status_ok_WHEN_mapped_THEN_returns_battery_low() {
        // BSB keeps status OK while still refusing to install, so isAllowed is the only usable gate
        val result = createUpdateStatus(isAllowed = false).toBsbUpdateStatus()

        assertEquals(BsbUpdateStatus.ReadyToInstall.BatteryLow, result)
    }

    @Test
    fun GIVEN_install_not_allowed_and_status_battery_low_WHEN_mapped_THEN_returns_battery_low() {
        val result = createUpdateStatus(
            isAllowed = false,
            status = UpdateStatus.Install.Status.BATTERY_LOW
        ).toBsbUpdateStatus()

        assertEquals(BsbUpdateStatus.ReadyToInstall.BatteryLow, result)
    }

    @Test
    fun GIVEN_stopped_session_and_install_not_allowed_WHEN_mapped_THEN_returns_battery_low() {
        val result = createUpdateStatus(
            isAllowed = false,
            event = UpdateStatus.Install.Event.SESSION_STOP,
            action = UpdateStatus.Install.Action.DOWNLOAD
        ).toBsbUpdateStatus()

        assertEquals(BsbUpdateStatus.ReadyToInstall.BatteryLow, result)
    }

    @Test
    fun GIVEN_download_in_progress_and_install_not_allowed_WHEN_mapped_THEN_returns_downloading() {
        val result = createUpdateStatus(
            isAllowed = false,
            event = UpdateStatus.Install.Event.ACTION_PROGRESS,
            action = UpdateStatus.Install.Action.DOWNLOAD,
            receivedBytes = 512,
            totalBytes = 1024
        ).toBsbUpdateStatus()

        assertEquals(
            BsbUpdateStatus.InProgress.Downloading.Specified(
                speedBytesPerSec = 0,
                receivedBytes = 512,
                totalBytes = 1024
            ),
            result
        )
    }

    @Test
    fun GIVEN_apply_in_progress_WHEN_mapped_THEN_returns_apply_stage() {
        val result = createUpdateStatus(
            isAllowed = true,
            event = UpdateStatus.Install.Event.ACTION_BEGIN,
            action = UpdateStatus.Install.Action.APPLY
        ).toBsbUpdateStatus()

        assertEquals(
            BsbUpdateStatus.InProgress.Other(
                BsbUpdateStatus.InProgress.Other.ProgressStage.APPLY
            ),
            result
        )
    }
}
