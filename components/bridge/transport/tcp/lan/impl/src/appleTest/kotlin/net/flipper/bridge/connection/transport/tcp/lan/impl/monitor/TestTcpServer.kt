@file:OptIn(ExperimentalForeignApi::class)
@file:Suppress("MagicNumber")

package net.flipper.bridge.connection.transport.tcp.lan.impl.monitor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import platform.posix.AF_INET
import platform.posix.F_GETFL
import platform.posix.F_SETFL
import platform.posix.INADDR_LOOPBACK
import platform.posix.O_NONBLOCK
import platform.posix.SOCK_STREAM
import platform.posix.SOL_SOCKET
import platform.posix.SO_LINGER
import platform.posix.accept
import platform.posix.bind
import platform.posix.close
import platform.posix.fcntl
import platform.posix.linger
import platform.posix.listen
import platform.posix.setsockopt
import platform.posix.sockaddr_in
import platform.posix.socket

// htons/htonl are macros on Darwin and not imported by cinterop.
// macOS ARM64 is little-endian; network byte order is big-endian.
private fun swapUShort(value: UShort): UShort {
    return ((value.toInt() shr 8) or ((value.toInt() and 0xFF) shl 8)).toUShort()
}

private fun swapUInt(value: UInt): UInt {
    return (
        (value shr 24) or
            ((value shr 8) and 0xFF00u) or
            ((value shl 8) and 0xFF0000u) or
            (value shl 24)
        )
}

/**
 * A minimal TCP server for testing. Binds to localhost on a random available port.
 * On stop(), drains and forcefully RST-closes all pending/accepted connections
 * so that the client side detects disconnection immediately.
 */
class TestTcpServer {
    private var serverFd: Int = -1

    var port: Int = -1
        private set

    fun start() {
        for (candidatePort in 50000..50500) {
            val fd = tryBind(candidatePort)
            if (fd >= 0) {
                serverFd = fd
                port = candidatePort
                return
            }
        }
        error("Could not bind to any port in range 50000..50500")
    }

    private fun tryBind(targetPort: Int): Int = memScoped {
        val fd = socket(AF_INET, SOCK_STREAM, 0)
        if (fd < 0) return -1

        val addr = alloc<sockaddr_in>()
        addr.sin_family = AF_INET.convert()
        addr.sin_port = swapUShort(targetPort.toUShort())
        addr.sin_addr.s_addr = swapUInt(INADDR_LOOPBACK)

        val bindResult = bind(
            fd,
            addr.ptr.reinterpret(),
            sizeOf<sockaddr_in>().convert()
        )
        if (bindResult != 0) {
            close(fd)
            return -1
        }

        val listenResult = listen(fd, 5)
        if (listenResult != 0) {
            close(fd)
            return -1
        }

        fd
    }

    /**
     * Force-close a client socket by sending TCP RST instead of graceful FIN.
     * SO_LINGER with l_linger=0 causes close() to send RST immediately.
     */
    private fun rstClose(fd: Int) = memScoped {
        val lingerOpt = alloc<linger>()
        lingerOpt.l_onoff = 1
        lingerOpt.l_linger = 0
        setsockopt(fd, SOL_SOCKET, SO_LINGER, lingerOpt.ptr, sizeOf<linger>().convert())
        close(fd)
    }

    fun stop() {
        if (serverFd < 0) return

        // Set non-blocking to drain the accept queue without blocking
        val flags = fcntl(serverFd, F_GETFL)
        fcntl(serverFd, F_SETFL, flags or O_NONBLOCK)

        // Drain all pending connections from the kernel backlog and RST them
        while (true) {
            val clientFd = accept(serverFd, null, null)
            if (clientFd < 0) break
            rstClose(clientFd)
        }

        close(serverFd)
        serverFd = -1
    }
}
