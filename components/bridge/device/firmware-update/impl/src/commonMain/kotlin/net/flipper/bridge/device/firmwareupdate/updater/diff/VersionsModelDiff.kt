package net.flipper.bridge.device.firmwareupdate.updater.diff

import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarVersion
import net.flipper.bridge.device.firmwareupdate.updater.model.BusyBarVersionTransition

object VersionsModelDiff {
    fun compareAndGet(
        localVersionModelOrNull: BusyBarVersionTransition?,
        newCurrentVersion: BusyBarVersion?
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
