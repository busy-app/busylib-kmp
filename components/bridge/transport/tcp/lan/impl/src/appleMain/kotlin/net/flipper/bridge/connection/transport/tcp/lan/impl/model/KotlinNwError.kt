package net.flipper.bridge.connection.transport.tcp.lan.impl.model

import platform.Network.nw_error_get_error_code
import platform.Network.nw_error_t

private const val ERROR_CODE_HOST_IS_DOWN = 64
private const val ERROR_CODE_NO_ROUTE_TO_HOST = 65
private const val ERROR_CODE_RESET_BY_PEER = 54
private const val ERROR_CODE_TIMED_OUT = 60

internal sealed class KotlinNwError(
    instance: nw_error_t,
) : Throwable(instance.toString()) {
    val code: Int = nw_error_get_error_code(instance)

    class HostIsDown(e: nw_error_t) : KotlinNwError(e)
    class NoRouteToHost(e: nw_error_t) : KotlinNwError(e)
    class ResetByPeer(e: nw_error_t) : KotlinNwError(e)
    class TimedOut(e: nw_error_t) : KotlinNwError(e)
    class Unknown(e: nw_error_t) : KotlinNwError(e)

    override fun toString(): String {
        return "${this::class} $message-$code"
    }
}

/**
 * **Important:** The nullable receiver type (`nw_error_t?`) must be preserved
 */
internal fun nw_error_t?.asKotlinNwError(): KotlinNwError? {
    if (this == null) return null
    val code = this.let(::nw_error_get_error_code)
    return when (code) {
        ERROR_CODE_HOST_IS_DOWN -> KotlinNwError.HostIsDown(this)
        ERROR_CODE_NO_ROUTE_TO_HOST -> KotlinNwError.NoRouteToHost(this)
        ERROR_CODE_RESET_BY_PEER -> KotlinNwError.ResetByPeer(this)
        ERROR_CODE_TIMED_OUT -> KotlinNwError.TimedOut(this)
        else -> KotlinNwError.Unknown(this)
    }
}
