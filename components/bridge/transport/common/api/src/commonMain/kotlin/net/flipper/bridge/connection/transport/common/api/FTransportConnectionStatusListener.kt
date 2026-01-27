package net.flipper.bridge.connection.transport.common.api

fun interface FTransportConnectionStatusListener {
    suspend fun onStatusUpdate(status: FInternalTransportConnectionStatus)
}
