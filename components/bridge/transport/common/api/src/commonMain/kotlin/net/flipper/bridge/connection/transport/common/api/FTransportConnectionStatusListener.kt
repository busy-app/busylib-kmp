package net.flipper.bridge.connection.transport.common.api

fun interface FTransportConnectionStatusListener {
    fun onStatusUpdate(status: FInternalTransportConnectionStatus)
}
