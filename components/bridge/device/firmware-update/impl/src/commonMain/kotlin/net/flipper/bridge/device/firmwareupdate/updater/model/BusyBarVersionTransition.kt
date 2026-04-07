package net.flipper.bridge.device.firmwareupdate.updater.model

import net.flipper.bridge.connection.feature.info.model.BsbBusyBarVersion

internal data class BusyBarVersionTransition(
    val previousVersion: BsbBusyBarVersion?,
    val currentVersion: BsbBusyBarVersion
)
