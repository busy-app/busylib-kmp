package net.flipper.bridge.connection.transport.common.api

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

    suspend fun disconnect()
}
