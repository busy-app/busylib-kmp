package net.flipper.bridge.lanmonitor.impl.platform

/**
 * Probes whether the BUSY Bar device is reachable over the local network.
 *
 * Implementations must never throw: a failed probe (timeout, refused connection,
 * unresolved host, …) is reported as [isReachable] returning `false`.
 */
interface LanReachabilityProbe {
    suspend fun isReachable(): Boolean
}
