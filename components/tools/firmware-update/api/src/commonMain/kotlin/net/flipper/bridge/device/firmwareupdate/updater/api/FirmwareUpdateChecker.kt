package net.flipper.bridge.device.firmwareupdate.updater.api

import net.flipper.bridge.device.firmwareupdate.updater.model.FirmwareUpdateCheckError
import net.flipper.busylib.core.wrapper.CResult

interface FirmwareUpdateChecker {
    /**
     * @return failure with [FirmwareUpdateCheckError] or the request error when checking fails.
     */
    suspend fun checkForUpdates(): CResult<Unit>
}
