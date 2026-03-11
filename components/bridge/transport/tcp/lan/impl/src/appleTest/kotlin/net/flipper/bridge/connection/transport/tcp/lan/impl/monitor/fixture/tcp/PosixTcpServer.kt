package net.flipper.bridge.connection.transport.tcp.lan.impl.monitor.fixture.tcp

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

/**
 * A minimal TCP server for testing. Binds to localhost on a random available port.
 * On stop(), drains and forcefully RST-closes all pending/accepted connections
 * so that the client side detects disconnection immediately.
 */
@OptIn(ExperimentalForeignApi::class)
@Suppress("MagicNumber")
class PosixTcpServer {
    /**
     * POSIX file descriptor of the listening server socket, or `-1` if not bound.
     */
    private var serverFd: Int = -1

    var port: Int = -1
        private set

    /**
     * Manual `htons` — converts a 16-bit value from host (little-endian) to network (big-endian) byte order.
     * Needed because Darwin exposes `htons` as a macro, which cinterop cannot import.
     */
    private fun swapUShort(value: UShort): UShort {
        return ((value.toInt() shr 8) or ((value.toInt() and 0xFF) shl 8)).toUShort()
    }

    /**
     * Manual `htonl` — converts a 32-bit value from host
     * (little-endian) to network (big-endian) byte order.
     */
    private fun swapUInt(value: UInt): UInt {
        return (
            (value shr 24) or
                ((value shr 8) and 0xFF00u) or
                ((value shl 8) and 0xFF0000u) or
                (value shl 24)
            )
    }

    /**
     * Force-close a client socket by sending TCP RST instead of graceful FIN.
     * SO_LINGER with l_linger=0 causes close() to send RST immediately.
     */
    private fun rstClose(fd: Int) = memScoped {
        val lingerOpt = alloc<linger>().apply {
            l_onoff = 1
            l_linger = 0
        }
        setsockopt(
            fd,
            SOL_SOCKET,
            SO_LINGER,
            lingerOpt.ptr,
            sizeOf<linger>().convert()
        )
        close(fd)
    }

    /**
     * Attempts to create, bind, and listen on a TCP socket at [targetPort] on localhost.
     * @return the socket file descriptor on success, or `-1` if the port is unavailable.
     */
    private fun tryBind(targetPort: Int): Int {
        return memScoped {
            val fd = socket(
                AF_INET,
                SOCK_STREAM,
                0
            )
            if (fd < 0) return@memScoped -1

            val addr = alloc<sockaddr_in>().apply {
                sin_family = AF_INET.convert()
                sin_port = swapUShort(targetPort.toUShort())
                sin_addr.s_addr = swapUInt(INADDR_LOOPBACK)
            }

            val bindResult = bind(
                fd,
                addr.ptr.reinterpret(),
                sizeOf<sockaddr_in>().convert()
            )
            if (bindResult != 0) {
                close(fd)
                return@memScoped -1
            }

            val listenResult = listen(fd, 5)
            if (listenResult != 0) {
                close(fd)
                return@memScoped -1
            }

            return@memScoped fd
        }
    }

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
