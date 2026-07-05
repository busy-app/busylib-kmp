package net.flipper.bridge.device.firmwareupdate.updater.api

import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateEvent
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow

interface FirmwareUpdaterApi {
    val state: WrappedStateFlow<FwUpdateState>
    val events: WrappedFlow<FwUpdateEvent>

    /**
     * `BUSYBar.uniqueId` of the device the currently active firmware update belongs to,
     * or `null` when no app-initiated update is in progress.
     *
     * The updater is app-wide (SingleIn) and its [state]/[events] are a single global
     * stream, so after a device switch the UI cannot otherwise tell "my update" from another
     * device's update. Consumers gate on `updatingDeviceId == currentDeviceId` (MOB-2777).
     */
    val updatingDeviceId: WrappedStateFlow<String?>

    /**
     * Force stop firmware download if possible
     */
    suspend fun stopFirmwareUpdate(): CResult<Unit>
    suspend fun startUpdateInstall(): CResult<Unit>

    /**
     * Required for button on Firmware settings screen
     */
    suspend fun checkForUpdates(): CResult<Unit>
}
