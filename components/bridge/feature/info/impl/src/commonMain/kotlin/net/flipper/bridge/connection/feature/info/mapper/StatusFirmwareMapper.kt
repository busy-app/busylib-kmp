package net.flipper.bridge.connection.feature.info.mapper

import net.flipper.bridge.connection.feature.info.model.BsbStatusFirmware
import net.flipper.bridge.connection.feature.rpc.api.model.StatusFirmware

internal fun StatusFirmware.toBsbStatusFirmware(): BsbStatusFirmware {
    return BsbStatusFirmware(
        version = version,
        target = target,
        branch = branch,
        buildDate = buildDate,
        commitHash = commitHash,
        nwpVersion = nwpVersion,
        matterVersion = matterVersion
    )
}
