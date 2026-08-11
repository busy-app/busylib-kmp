package net.flipper.bridge.device.firmwareupdate.updater.api

import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateEvent
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateTrack
import net.flipper.bridge.device.firmwareupdate.updater.model.StartUpdateResponse
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow

interface FirmwareUpdaterApi {
    val state: WrappedStateFlow<FwUpdateState>
    val events: WrappedFlow<FwUpdateEvent>

    /**
     * Updates tracked by the library, keyed by device uniqueId. An entry survives
     * disconnects and device switches while its update keeps running on the device
     * itself, and is removed when the update finishes, fails, or is stopped
     */
    val updatingDevices: WrappedStateFlow<Map<String, FwUpdateTrack>>

    /**
     * Best-effort cancel of the firmware update on [deviceId]: aborts the device-side
     * download when reachable and always releases the local update tracking for that device
     */
    suspend fun stopFirmwareUpdate(deviceId: String): CResult<Unit>
    suspend fun startUpdateInstall(): StartUpdateResponse

    /**
     * Required for button on Firmware settings screen
     */
    suspend fun checkForUpdates(): CResult<Unit>
}
