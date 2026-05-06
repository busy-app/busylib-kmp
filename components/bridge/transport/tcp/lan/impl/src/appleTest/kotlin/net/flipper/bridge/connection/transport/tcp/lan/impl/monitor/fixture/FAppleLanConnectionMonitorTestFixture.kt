package net.flipper.bridge.connection.transport.tcp.lan.impl.monitor.fixture

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig
import net.flipper.bridge.connection.transport.tcp.lan.impl.monitor.FAppleLanConnectionMonitor
import net.flipper.bridge.connection.transport.tcp.lan.impl.monitor.fixture.api.FakeLanApi
import net.flipper.bridge.connection.transport.tcp.lan.impl.monitor.fixture.api.RecordingStatusListener
import net.flipper.bridge.connection.transport.tcp.lan.impl.monitor.fixture.tcp.PosixTcpServer
import platform.Foundation.NSThread

class FAppleLanConnectionMonitorTestFixture {
    val server: PosixTcpServer = PosixTcpServer()
    val listener: RecordingStatusListener = RecordingStatusListener()
    val deviceApi: FakeLanApi = FakeLanApi()
    private val monitors = mutableListOf<FAppleLanConnectionMonitor>()

    fun createMonitor(
        scope: CoroutineScope,
        host: String = "127.0.0.1",
        port: String = server.port.toString()
    ): FAppleLanConnectionMonitor {
        val config = FLanDeviceConnectionConfig(
            host = host,
            name = "test-device"
        )
        val monitor = FAppleLanConnectionMonitor(
            listener = listener,
            config = config,
            scope = scope,
            deviceApi = deviceApi,
            port = port
        )
        monitors.add(monitor)
        return monitor
    }

    fun dispose() {
        monitors.forEach(FAppleLanConnectionMonitor::stopMonitoring)
        monitors.clear()
        NSThread.sleepForTimeInterval(1.0)
        server.stop()
    }
}
