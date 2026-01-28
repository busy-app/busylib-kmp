package net.flipper.bridge.connection.transport.combined.impl.connections.helpers

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener

/**
 * Mock implementation of
 * [net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection]
 * for testing connection scenarios.
 *
 * Features:
 * - Track connection attempts count
 * - Simulate connection delays
 * - Simulate connection failures (for N attempts or always)
 * - Wait for specific connection attempt numbers
 * - Access listeners for simulating status updates
 * - Optional callback on connect for custom test scenarios
 */
class MockConnectionBuilder : FDeviceConfigToConnection {
    /** Number of connection attempts made */
    var connectAttempts = 0
        private set

    /** All listeners registered during connection attempts */
    val listeners = mutableListOf<FTransportConnectionStatusListener>()

    /** Deferred that completes when first connection is attempted */
    val connectCalledDeferred = CompletableDeferred<Unit>()

    /** Delay to apply during connection (simulates slow connections) */
    var connectDelay: Long = 0L

    /** Number of times connection should fail before succeeding (0 = never fail) */
    var shouldFailTimes = 0

    /** Whether connection should always fail */
    var shouldAlwaysFail = false

    /** Exception to throw on failure */
    var failureException: Throwable = RuntimeException("Connection failed")

    /** All device APIs created during connections */
    val deviceApis = mutableListOf<TestConnectedDeviceApi>()

    /** Optional callback invoked at the start of each connect call */
    var onConnectCallback: (suspend () -> Unit)? = null

    /** Template device API - used when creating new connections */
    var templateDeviceApi: TestConnectedDeviceApi? = null

    // Allows test to wait for specific number of connection attempts
    private val attemptDeferreds = mutableMapOf<Int, CompletableDeferred<Unit>>()

    /**
     * Returns a deferred that completes when the specified attempt number is reached.
     *
     * @param attemptNumber The attempt number to wait for (1-based)
     */
    fun waitForAttempt(attemptNumber: Int): CompletableDeferred<Unit> {
        return attemptDeferreds.getOrPut(attemptNumber) { CompletableDeferred() }
    }

    /**
     * Gets the most recent listener, or null if no connections have been made.
     */
    fun latestListener(): FTransportConnectionStatusListener? = listeners.lastOrNull()

    /**
     * Resets the builder state for reuse in tests.
     */
    fun reset() {
        connectAttempts = 0
        listeners.clear()
        deviceApis.clear()
        attemptDeferreds.clear()
        shouldFailTimes = 0
        shouldAlwaysFail = false
        connectDelay = 0L
        onConnectCallback = null
        templateDeviceApi = null
    }

    override suspend fun <API : FConnectedDeviceApi, CONFIG : FDeviceConnectionConfig<API>> connect(
        scope: CoroutineScope,
        config: CONFIG,
        listener: FTransportConnectionStatusListener
    ): Result<API> {
        connectAttempts++
        listeners.add(listener)

        // Notify first connection
        if (connectAttempts == 1) {
            connectCalledDeferred.complete(Unit)
        }

        // Notify specific attempt waiters
        attemptDeferreds[connectAttempts]?.complete(Unit)

        // Invoke custom callback if set
        onConnectCallback?.invoke()

        // Apply connection delay
        if (connectDelay > 0) delay(connectDelay)

        // Check if should fail
        if (shouldAlwaysFail || connectAttempts <= shouldFailTimes) {
            return Result.failure(failureException)
        }

        // Create and return device API
        val deviceApi = templateDeviceApi?.copy()
            ?: TestConnectedDeviceApi("Device-$connectAttempts")
        deviceApis.add(deviceApi)

        @Suppress("UNCHECKED_CAST")
        return Result.success(deviceApi as API)
    }

    /**
     * Helper to copy a TestConnectedDeviceApi with the same settings.
     */
    private fun TestConnectedDeviceApi.copy(): TestConnectedDeviceApi {
        return TestConnectedDeviceApi(deviceName).apply {
            disconnectDelay = this@copy.disconnectDelay
        }
    }
}
