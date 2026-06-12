package net.flipper.bridge.lanmonitor.impl.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TcpSocketLanReachabilityProbeTest {

    @Test
    fun GIVEN_open_tcp_port_WHEN_probe_THEN_reachable() = runTest {
        ServerSocket(0).use { server ->
            val probe = TcpSocketLanReachabilityProbe(
                host = LOCALHOST,
                port = server.localPort,
                connectTimeoutMs = CONNECT_TIMEOUT_MS,
                ioDispatcher = Dispatchers.IO,
            )

            assertTrue(probe.isReachable())
        }
    }

    @Test
    fun GIVEN_closed_tcp_port_WHEN_probe_THEN_not_reachable() = runTest {
        val closedPort = ServerSocket(0).use { server -> server.localPort }
        val probe = TcpSocketLanReachabilityProbe(
            host = LOCALHOST,
            port = closedPort,
            connectTimeoutMs = CONNECT_TIMEOUT_MS,
            ioDispatcher = Dispatchers.IO,
        )

        assertFalse(probe.isReachable())
    }

    private companion object {
        const val LOCALHOST = "127.0.0.1"
        const val CONNECT_TIMEOUT_MS = 2000
    }
}
