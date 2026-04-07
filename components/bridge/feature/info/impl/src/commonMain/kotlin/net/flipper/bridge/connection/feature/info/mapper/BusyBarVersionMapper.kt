package net.flipper.bridge.connection.feature.info.mapper

import net.flipper.bridge.connection.feature.info.model.BsbBusyBarVersion
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarVersion

internal fun BusyBarVersion.toBsbBusyBarVersion(): BsbBusyBarVersion {
    return BsbBusyBarVersion(version = version)
}
