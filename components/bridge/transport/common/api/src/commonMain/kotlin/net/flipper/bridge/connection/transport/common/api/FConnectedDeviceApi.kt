package net.flipper.bridge.connection.transport.common.api

interface FConnectedDeviceApi {
    val deviceName: String

    suspend fun disconnect()
}
