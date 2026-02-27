package net.flipper.bridge.device.firmwareupdate.updater.api

import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedStateFlow

interface FirmwareUpdaterApi {
    val state: WrappedStateFlow<FwUpdateState>

    /**
     * Force stop firmware download if possible
     */
    suspend fun stopFirmwareUpdate(): CResult<Unit>
    suspend fun startUpdateInstall(): CResult<Unit>
}
