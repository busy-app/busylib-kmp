package net.flipper.bridge.connection.transport.combined.impl

import net.flipper.bridge.connection.transport.combined.impl.connections.ConnectionSnapshot
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.core.busylib.data.nonEmptyListOf

internal fun mergeSnapshots(snapshots: List<ConnectionSnapshot>): ConnectionSnapshot {
    if (snapshots.size == 1) return snapshots.single()

    val base = snapshots.first()
    val combinedCapabilities = snapshots
        .flatMap { it.capabilities.orEmpty() }
        .distinct()
        .takeIf { it.isNotEmpty() }

    val combinedStatus = when (val baseStatus = base.status) {
        is FInternalTransportConnectionStatus.Connected -> {
            val allTypes = snapshots
                .flatMap { (it.status as FInternalTransportConnectionStatus.Connected).connectionType }
                .distinct()
            baseStatus.copy(
                connectionType = nonEmptyListOf(allTypes.first(), allTypes.drop(1))
            )
        }
        is FInternalTransportConnectionStatus.Connecting -> {
            val allTypes = snapshots
                .flatMap { (it.status as FInternalTransportConnectionStatus.Connecting).connectionType }
                .distinct()
            baseStatus.copy(
                connectionType = nonEmptyListOf(allTypes.first(), allTypes.drop(1))
            )
        }
        else -> baseStatus
    }

    return ConnectionSnapshot(
        capabilities = combinedCapabilities,
        status = combinedStatus
    )
}

@Suppress("MagicNumber")
internal fun getPriority(status: FInternalTransportConnectionStatus): Int {
    return when (status) {
        FInternalTransportConnectionStatus.Disconnected -> 0
        is FInternalTransportConnectionStatus.Connecting -> 1
        FInternalTransportConnectionStatus.Disconnecting -> 2
        is FInternalTransportConnectionStatus.Connected -> 3
    }
}
