package net.flipper.bridge.connection.feature.info.mapper

import net.flipper.bridge.connection.feature.info.model.BsbBusyBarStatusSystem
import net.flipper.bridge.connection.feature.rpc.generated.model.StatusSystem

internal fun StatusSystem.toBsbBusyBarStatusSystem(): BsbBusyBarStatusSystem {
    return BsbBusyBarStatusSystem(
        apiSemver = apiSemver,
        uptime = uptime,
        bootTime = bootTime
    )
}
