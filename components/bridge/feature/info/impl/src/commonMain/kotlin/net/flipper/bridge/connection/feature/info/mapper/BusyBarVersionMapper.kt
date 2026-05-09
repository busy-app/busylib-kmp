package net.flipper.bridge.connection.feature.info.mapper

import net.flipper.bridge.connection.feature.info.model.BsbBusyBarVersion
import net.flipper.bridge.connection.feature.rpc.generated.model.VersionInfo

internal fun VersionInfo.toBsbBusyBarVersion(): BsbBusyBarVersion {
    return BsbBusyBarVersion(version = apiSemver)
}
