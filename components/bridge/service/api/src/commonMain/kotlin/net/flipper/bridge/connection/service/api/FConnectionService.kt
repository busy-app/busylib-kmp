package net.flipper.bridge.connection.service.api

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
     * Forget current device and disconnect from it as side effect
     */
    fun forgetCurrentDevice()
}
