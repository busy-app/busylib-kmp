package net.flipper.bridge.connection.transport.combined.impl.connections.helpers

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig

/**
 * Mock implementation of [FConnectedDeviceApi] for testing.
 *
 * @property deviceName The name of the test device
 */
class TestConnectedDeviceApi(
    override val deviceName: String = "TestDevice"
) : FConnectedDeviceApi {
    var tryUpdateResult: Result<Unit> = Result.success(Unit)
    var tryUpdateConfigReceived: FDeviceConnectionConfig<*>? = null
    var tryUpdateCallCount = 0

    override suspend fun tryUpdateConnectionConfig(config: FDeviceConnectionConfig<*>): Result<Unit> {
        tryUpdateCallCount++
        tryUpdateConfigReceived = config
        return tryUpdateResult
    }

    var disconnectCalled = false
    var disconnectDelay: Long = 0L
    val disconnectCalledDeferred = CompletableDeferred<Unit>()

    override suspend fun disconnect() {
        if (disconnectDelay > 0) {
            delay(disconnectDelay)
        }
        disconnectCalled = true
        disconnectCalledDeferred.complete(Unit)
    }
}
