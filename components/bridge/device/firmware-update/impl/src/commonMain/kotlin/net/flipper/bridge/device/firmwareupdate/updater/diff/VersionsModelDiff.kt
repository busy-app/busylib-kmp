package net.flipper.bridge.device.firmwareupdate.updater.diff

import net.flipper.bridge.connection.feature.info.model.BsbBusyBarVersion
import net.flipper.bridge.device.firmwareupdate.updater.model.BusyBarVersionTransition

internal object VersionsModelDiff {
    fun compareAndGet(
        localVersionModelOrNull: BusyBarVersionTransition?,
        newCurrentVersion: BsbBusyBarVersion?
    ): BusyBarVersionTransition? {
        return when {
            newCurrentVersion == null -> {
                localVersionModelOrNull
            }

            localVersionModelOrNull == null -> {
                val newVersionModel = BusyBarVersionTransition(
                    previousVersion = null,
                    currentVersion = newCurrentVersion
                )
                newVersionModel
            }

            localVersionModelOrNull.currentVersion == newCurrentVersion -> {
                val newVersionModel = BusyBarVersionTransition(
                    previousVersion = localVersionModelOrNull.currentVersion,
                    currentVersion = newCurrentVersion
                )
                newVersionModel
            }

            localVersionModelOrNull.currentVersion != newCurrentVersion -> {
                val newVersionModel = BusyBarVersionTransition(
                    previousVersion = localVersionModelOrNull.currentVersion,
                    currentVersion = newCurrentVersion
                )
                newVersionModel
            }

            else -> {
                localVersionModelOrNull
            }
        }
    }
}
