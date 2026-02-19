package net.flipper.bridge.connection.service.api

import net.flipper.bridge.connection.config.api.model.BUSYBar

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
    suspend fun forgetDevice(device: BUSYBar): Result<Unit>

    /**
     * Forget current device and disconnect from it
     */
    suspend fun forgetCurrentDevice(): Result<Unit>
}
