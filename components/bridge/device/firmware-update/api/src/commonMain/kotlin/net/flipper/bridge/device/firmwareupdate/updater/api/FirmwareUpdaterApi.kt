package net.flipper.bridge.device.firmwareupdate.updater.api

import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateEvent
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow

interface FirmwareUpdaterApi {
    val state: WrappedStateFlow<FwUpdateState>
    val events: WrappedFlow<FwUpdateEvent>

    val updatingDeviceId: WrappedStateFlow<String?>

    /**
     * Best-effort cancel of the firmware update on [deviceId]: aborts the device-side
     * download when that device is reachable, cancels the app-side LAN update job it owns,
     * and always releases the local update tracking ([updatingDeviceId], install gate)
     * for that device — even when the device cannot be reached
     */
    suspend fun stopFirmwareUpdate(deviceId: String): CResult<Unit>
    suspend fun startUpdateInstall(): CResult<Unit>

    /**
     * Required for button on Firmware settings screen
     */
    suspend fun checkForUpdates(): CResult<Unit>
}
