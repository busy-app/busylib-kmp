package net.flipper.bridge.connection.service.api

/**
 * This service handle connection for current flipper device
 */
interface FConnectionService {
    /**
     * Should be called once on application start
     */
    fun onApplicationInit()

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
