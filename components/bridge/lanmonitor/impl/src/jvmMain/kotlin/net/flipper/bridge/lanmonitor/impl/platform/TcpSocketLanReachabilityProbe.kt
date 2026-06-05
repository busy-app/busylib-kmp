package net.flipper.bridge.lanmonitor.impl.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import java.net.InetSocketAddress
import java.net.Socket

/**
 * [LanReachabilityProbe] that opens a raw TCP connection to the BUSY Bar device
 * using pure JVM sockets (no platform-native networking).
 *
 * A successful TCP handshake within [connectTimeoutMs] means the device is
 * reachable; any failure (timeout, refused, unresolved host) means it is not.
 */
class TcpSocketLanReachabilityProbe(
    private val host: String = DEFAULT_HOST,
    private val port: Int = DEFAULT_PORT,
    private val connectTimeoutMs: Int = CONNECT_TIMEOUT_MS,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LanReachabilityProbe {

    override suspend fun isReachable(): Boolean {
        return runSuspendCatching(dispatcher = ioDispatcher) {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), connectTimeoutMs)
            }
        }.isSuccess
    }

    companion object {
        const val DEFAULT_HOST: String = "10.0.4.20"
        const val DEFAULT_PORT: Int = 80
        const val CONNECT_TIMEOUT_MS: Int = 2000
    }
}
