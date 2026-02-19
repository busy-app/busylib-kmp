package net.flipper.bridge.connection.service.api

import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.busylib.core.wrapper.CResult

/**
 * This service handle connection for current flipper device
 */
interface FConnectionService {
    /**
     * Reconnect to last known device after being force disconnected
     */
    fun connectCurrent()

    /**
     * Disconnect current device
     */
    fun disconnect()

    /**
     * Forget selected [device] and disconnect from it
     */
    suspend fun forgetDevice(device: BUSYBar): CResult<Unit>
}
