package net.flipper.bridge.connection.feature.info.mapper

import net.flipper.bridge.connection.feature.info.model.BsbBusyBarStatusSystem
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusSystem

internal fun BusyBarStatusSystem.toBsbBusyBarStatusSystem(): BsbBusyBarStatusSystem {
    return BsbBusyBarStatusSystem(
        apiSemver = apiSemver,
        uptime = uptime,
        bootTime = bootTime
    )
}
