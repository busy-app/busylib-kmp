package net.flipper.bridge.connection.transport.tcp.common.monitor

interface FConnectionMonitorApi {
    suspend fun startMonitoring()
    fun stopMonitoring()
}
