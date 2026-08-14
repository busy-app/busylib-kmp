package net.flipper.bridge.device.firmwareupdate.updater.api

import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.device.firmwareupdate.status.model.UpdateStatusSource
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateTrack

/**
 * Latches devices whose update was started outside this client (from the device itself
 * or another client). Such an update never goes through
 * [FirmwareUpdaterApi.startUpdateInstall], so nothing else would ever track it — and
 * without a track the update UI dies on the first switch-away: the app-wide state
 * stream restarts per selection and forgets the device is mid-update.
 */
internal object RemoteUpdateTrackLatch {

    /**
     * [tracks] with a [FwUpdateTrack.InstallType.REMOTE] track added when [source] is a
     * fresh, device-reported in-progress status of an untracked device; [tracks] itself
     * otherwise.
     *
     * Only [UpdateStatusSource.Fresh] latches: a Cached status is only ever built from a
     * previous Fresh of the same selection, so its Fresh emission has already been seen.
     * An existing track is never replaced — it may carry [FwUpdateTrack.targetOutOfSight],
     * which a rebuilt track would silently reset.
     */
    fun tracksAfter(
        source: UpdateStatusSource,
        tracks: Map<String, FwUpdateTrack>
    ): Map<String, FwUpdateTrack> {
        val status = (source as? UpdateStatusSource.Fresh)?.status ?: return tracks
        if (status.status !is BsbUpdateStatus.InProgress) return tracks
        if (status.deviceId in tracks) return tracks
        return tracks + (status.deviceId to FwUpdateTrack(FwUpdateTrack.InstallType.REMOTE))
    }
}
