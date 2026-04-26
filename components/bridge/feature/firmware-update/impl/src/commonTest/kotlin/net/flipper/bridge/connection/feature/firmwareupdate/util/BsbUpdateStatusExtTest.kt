package net.flipper.bridge.connection.feature.firmwareupdate.util

import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbCheck
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall.BsbAction
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall.BsbDownload
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall.BsbStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class BsbUpdateStatusExtTest {

    @Test
    fun GIVEN_update_state_event_with_download_WHEN_merge_THEN_download_overwrites_cache() {
        val cached = makeStatus(
            action = BsbAction.NONE,
            status = BsbStatus.OK,
            download = BsbDownload(speedBytesPerSec = 0, receivedBytes = 0, totalBytes = 0)
        )
        val polled = BsbDownload(speedBytesPerSec = 251_095, receivedBytes = 7_998_844, totalBytes = 9_620_874)

        val merged = cached.merge(
            BusyLibUpdateEvent.Update.UpdateState(
                action = BsbAction.DOWNLOAD,
                status = BsbStatus.OK,
                download = polled
            )
        )

        assertEquals(BsbAction.DOWNLOAD, merged.install.action)
        assertEquals(BsbStatus.OK, merged.install.status)
        assertEquals(polled, merged.install.download)
    }

    @Test
    fun GIVEN_update_state_event_without_download_WHEN_merge_THEN_cached_download_preserved() {
        val cachedDownload = BsbDownload(speedBytesPerSec = 12_345, receivedBytes = 6_789, totalBytes = 1_000_000)
        val cached = makeStatus(
            action = BsbAction.DOWNLOAD,
            status = BsbStatus.OK,
            download = cachedDownload
        )

        val merged = cached.merge(
            BusyLibUpdateEvent.Update.UpdateState(
                action = BsbAction.UNPACK,
                status = BsbStatus.OK,
                download = null
            )
        )

        assertEquals(BsbAction.UNPACK, merged.install.action)
        assertEquals(BsbStatus.OK, merged.install.status)
        assertEquals(cachedDownload, merged.install.download)
    }

    @Test
    fun GIVEN_update_check_event_with_version_WHEN_merge_THEN_available_version_overwritten() {
        val cached = makeStatus(check = BsbCheck(availableVersion = "old", status = BsbCheck.BsbCheckResult.NONE))

        val merged = cached.merge(BusyLibUpdateEvent.Update.UpdateCheck(availableVersion = "new"))

        assertEquals("new", merged.check.availableVersion)
    }

    @Test
    fun GIVEN_update_check_event_with_null_version_WHEN_merge_THEN_status_unchanged() {
        val cached = makeStatus(
            check = BsbCheck(availableVersion = "keep-me", status = BsbCheck.BsbCheckResult.AVAILABLE)
        )

        val merged = cached.merge(BusyLibUpdateEvent.Update.UpdateCheck(availableVersion = null))

        assertEquals(cached, merged)
    }

    private fun makeStatus(
        action: BsbAction = BsbAction.NONE,
        status: BsbStatus = BsbStatus.OK,
        download: BsbDownload = BsbDownload(speedBytesPerSec = 0, receivedBytes = 0, totalBytes = 0),
        check: BsbCheck = BsbCheck(availableVersion = "", status = BsbCheck.BsbCheckResult.NONE)
    ): BsbUpdateStatus = BsbUpdateStatus(
        install = BsbInstall(isAllowed = true, action = action, status = status, download = download),
        check = check
    )
}
