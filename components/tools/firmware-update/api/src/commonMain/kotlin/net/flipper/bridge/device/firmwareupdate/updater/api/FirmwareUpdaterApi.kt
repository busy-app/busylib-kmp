package net.flipper.bridge.device.firmwareupdate.updater.api

import net.flipper.bridge.device.firmwareupdate.updater.model.FirmwareUpdateCheckError
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateEvent
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.bridge.device.firmwareupdate.updater.model.StartUpdateResponse
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow

interface FirmwareUpdaterApi {
    val state: WrappedStateFlow<FwUpdateState>
    val events: WrappedFlow<FwUpdateEvent>

    /**
     * Force stop firmware download if possible
     */
    suspend fun stopFirmwareUpdate(): CResult<Unit>
    suspend fun startUpdateInstall(): StartUpdateResponse

    /** @return failure with [FirmwareUpdateCheckError.RpcUnavailable] or the request error if it cannot be sent. */
    suspend fun checkForUpdates(): CResult<Unit>

    /**
     * @return success after checking, including when no update is available;
     * failure with [FirmwareUpdateCheckError] or the request error when checking fails.
     */
    suspend fun checkForUpdatesAndAwaitCompletion(): CResult<Unit>
}
