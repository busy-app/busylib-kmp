package net.flipper.bridge.lanmonitor.impl.platform.model

import platform.Network.nw_connection_copy_current_path
import platform.Network.nw_connection_state_cancelled
import platform.Network.nw_connection_state_failed
import platform.Network.nw_connection_state_invalid
import platform.Network.nw_connection_state_preparing
import platform.Network.nw_connection_state_ready
import platform.Network.nw_connection_state_waiting
import platform.Network.nw_connection_t
import platform.Network.nw_error_get_error_code
import platform.Network.nw_error_t
import platform.Network.nw_path_get_unsatisfied_reason
import platform.Network.nw_path_unsatisfied_reason_cellular_denied
import platform.Network.nw_path_unsatisfied_reason_local_network_denied
import platform.Network.nw_path_unsatisfied_reason_wifi_denied

private const val ERROR_CODE_HOST_IS_DOWN = 64
private const val ERROR_CODE_NO_ROUTE_TO_HOST = 65
private const val ERROR_CODE_RESET_BY_PEER = 54
private const val ERROR_CODE_TIMED_OUT = 60

internal sealed class KotlinNwStatus {

    sealed class PosixError(instance: nw_error_t) {
        val code: Int = nw_error_get_error_code(instance)

        class HostIsDown(e: nw_error_t) : PosixError(e)
        class NoRouteToHost(e: nw_error_t) : PosixError(e)
        class ResetByPeer(e: nw_error_t) : PosixError(e)
        class TimedOut(e: nw_error_t) : PosixError(e)
        class Unknown(e: nw_error_t) : PosixError(e)

        override fun toString(): String = "${this::class} -> (code=$code)"
    }

    data object Ready : KotlinNwStatus()
    data object Preparing : KotlinNwStatus()
    data object Cancelled : KotlinNwStatus()
    data object Invalid : KotlinNwStatus()
    data class UnknownState(val rawState: UInt) : KotlinNwStatus()

    sealed class Waiting : KotlinNwStatus() {
        data object LocalNetworkDenied : Waiting()
        data object WifiDenied : Waiting()
        data object CellularDenied : Waiting()

        data class Other(val error: PosixError?) : Waiting()
    }

    data class Failed(val error: PosixError?) : KotlinNwStatus()
}

private fun nw_error_t.asPosixError(): KotlinNwStatus.PosixError {
    return when (nw_error_get_error_code(this)) {
        ERROR_CODE_HOST_IS_DOWN -> KotlinNwStatus.PosixError.HostIsDown(this)
        ERROR_CODE_NO_ROUTE_TO_HOST -> KotlinNwStatus.PosixError.NoRouteToHost(this)
        ERROR_CODE_RESET_BY_PEER -> KotlinNwStatus.PosixError.ResetByPeer(this)
        ERROR_CODE_TIMED_OUT -> KotlinNwStatus.PosixError.TimedOut(this)
        else -> KotlinNwStatus.PosixError.Unknown(this)
    }
}

private fun asWaitingStatus(
    connection: nw_connection_t,
    posix: KotlinNwStatus.PosixError?
): KotlinNwStatus.Waiting {
    val path = nw_connection_copy_current_path(connection)
        ?: return KotlinNwStatus.Waiting.Other(posix)
    return when (nw_path_get_unsatisfied_reason(path)) {
        nw_path_unsatisfied_reason_local_network_denied -> KotlinNwStatus.Waiting.LocalNetworkDenied
        nw_path_unsatisfied_reason_wifi_denied -> KotlinNwStatus.Waiting.WifiDenied
        nw_path_unsatisfied_reason_cellular_denied -> KotlinNwStatus.Waiting.CellularDenied
        else -> KotlinNwStatus.Waiting.Other(posix)
    }
}

internal fun nw_connection_t.asKotlinNwStatus(state: UInt, error: nw_error_t): KotlinNwStatus {
    val posixError = error?.asPosixError()
    return when (state) {
        nw_connection_state_failed -> KotlinNwStatus.Failed(posixError)
        nw_connection_state_waiting -> asWaitingStatus(this, posixError)
        nw_connection_state_ready ->
            posixError
                ?.let(KotlinNwStatus::Failed)
                ?: KotlinNwStatus.Ready

        nw_connection_state_preparing ->
            posixError
                ?.let(KotlinNwStatus::Failed)
                ?: KotlinNwStatus.Preparing

        nw_connection_state_cancelled ->
            posixError
                ?.let(KotlinNwStatus::Failed)
                ?: KotlinNwStatus.Cancelled

        nw_connection_state_invalid ->
            posixError
                ?.let(KotlinNwStatus::Failed)
                ?: KotlinNwStatus.Invalid

        else -> KotlinNwStatus.UnknownState(state)
    }
}
