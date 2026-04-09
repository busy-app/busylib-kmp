package net.flipper.bridge.connection.service.api

import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.service.model.ForgetDeviceResult

/**
 * This service handle connection for current flipper device
 */
interface FConnectionService {
    /**
     * Forget selected [device] and disconnect from it
     */
    suspend fun forgetDevice(device: BUSYBar): ForgetDeviceResult
}
