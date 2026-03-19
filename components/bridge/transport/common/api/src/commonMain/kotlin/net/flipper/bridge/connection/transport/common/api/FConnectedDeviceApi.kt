package net.flipper.bridge.connection.transport.common.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface FConnectedDeviceApi {
    val deviceName: String

    /**
     * Try to update connection config without interrupting connection
     *
     * If it's not possible, return Result.failure() with description
     */
    suspend fun tryUpdateConnectionConfig(
        config: FDeviceConnectionConfig<*>
    ): Result<Unit>

    fun getCurrentConnectionTypeFlow(): Flow<FInternalTransportConnectionType?> {
        return flowOf(null)
    }

    suspend fun disconnect()
}
