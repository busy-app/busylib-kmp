package net.flipper.bridge.device.firmwareupdate.updater.api

import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.model.DeviceUpdateStatus
import net.flipper.bridge.device.firmwareupdate.status.model.UpdateStatusSource
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateTrack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RemoteUpdateTrackLatchTest {

    @Test
    fun GIVEN_fresh_downloading_status_of_untracked_device_WHEN_tracksAfter_THEN_remote_track_added() {
        val source = UpdateStatusSource.Fresh(
            DeviceUpdateStatus("device-a", BsbUpdateStatus.InProgress.Downloading.NotSpecified)
        )

        val result = RemoteUpdateTrackLatch.tracksAfter(source, emptyMap())

        assertEquals(
            mapOf("device-a" to FwUpdateTrack(FwUpdateTrack.InstallType.REMOTE)),
            result
        )
    }

    @Test
    fun GIVEN_fresh_applying_status_of_untracked_device_WHEN_tracksAfter_THEN_remote_track_added() {
        val source = UpdateStatusSource.Fresh(
            DeviceUpdateStatus(
                "device-a",
                BsbUpdateStatus.InProgress.Other(BsbUpdateStatus.InProgress.Other.ProgressStage.APPLY)
            )
        )

        val result = RemoteUpdateTrackLatch.tracksAfter(source, emptyMap())

        assertEquals(
            mapOf("device-a" to FwUpdateTrack(FwUpdateTrack.InstallType.REMOTE)),
            result
        )
    }

    @Test
    fun GIVEN_in_progress_status_of_already_tracked_device_WHEN_tracksAfter_THEN_existing_track_untouched() {
        // A local DEFAULT track that already went out of sight — rebuilding it would reset the flag
        val tracks = mapOf(
            "device-a" to FwUpdateTrack(FwUpdateTrack.InstallType.DEFAULT, targetOutOfSight = true)
        )
        val source = UpdateStatusSource.Fresh(
            DeviceUpdateStatus("device-a", BsbUpdateStatus.InProgress.Downloading.NotSpecified)
        )

        val result = RemoteUpdateTrackLatch.tracksAfter(source, tracks)

        assertSame(tracks, result)
    }

    @Test
    fun GIVEN_in_progress_status_WHEN_tracksAfter_THEN_other_devices_tracks_preserved() {
        val tracks = mapOf(
            "device-b" to FwUpdateTrack(FwUpdateTrack.InstallType.LAN, targetOutOfSight = true)
        )
        val source = UpdateStatusSource.Fresh(
            DeviceUpdateStatus("device-a", BsbUpdateStatus.InProgress.Downloading.NotSpecified)
        )

        val result = RemoteUpdateTrackLatch.tracksAfter(source, tracks)

        assertEquals(
            mapOf(
                "device-b" to FwUpdateTrack(FwUpdateTrack.InstallType.LAN, targetOutOfSight = true),
                "device-a" to FwUpdateTrack(FwUpdateTrack.InstallType.REMOTE)
            ),
            result
        )
    }

    @Test
    fun GIVEN_fresh_idle_status_WHEN_tracksAfter_THEN_nothing_added() {
        val source = UpdateStatusSource.Fresh(
            DeviceUpdateStatus("device-a", BsbUpdateStatus.ReadyToInstall.Ready)
        )

        val result = RemoteUpdateTrackLatch.tracksAfter(source, emptyMap())

        assertSame(emptyMap(), result)
    }

    @Test
    fun GIVEN_fresh_loading_status_WHEN_tracksAfter_THEN_nothing_added() {
        val source = UpdateStatusSource.Fresh(
            DeviceUpdateStatus("device-a", BsbUpdateStatus.Loading)
        )

        val result = RemoteUpdateTrackLatch.tracksAfter(source, emptyMap())

        assertSame(emptyMap(), result)
    }

    @Test
    fun GIVEN_fresh_null_status_WHEN_tracksAfter_THEN_nothing_added() {
        val source = UpdateStatusSource.Fresh(null)

        val result = RemoteUpdateTrackLatch.tracksAfter(source, emptyMap())

        assertSame(emptyMap(), result)
    }

    @Test
    fun GIVEN_cached_in_progress_status_WHEN_tracksAfter_THEN_nothing_added() {
        // Cached is only ever built from a previous Fresh of the same selection,
        // so the Fresh emission has already latched — Cached must not re-latch
        val source = UpdateStatusSource.Cached(
            DeviceUpdateStatus("device-a", BsbUpdateStatus.InProgress.Downloading.NotSpecified)
        )

        val result = RemoteUpdateTrackLatch.tracksAfter(source, emptyMap())

        assertSame(emptyMap(), result)
    }
}
