package net.flipper.bridge.device.firmwareupdate.updater.model

import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarVersion

internal data class BusyBarVersionTransition(
    val previousVersion: BusyBarVersion?,
    val currentVersion: BusyBarVersion
)
