package net.flipper.bridge.connection.feature.firmwareupdate.util

import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.firmwareupdate.model.AvailableVersion
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus

fun UpdateStatus.toAvailableVersion(): AvailableVersion {
    return when (check.event) {
        UpdateStatus.Check.CheckEvent.START -> AvailableVersion.CheckingOnBBInProgress
        UpdateStatus.Check.CheckEvent.STOP,
        UpdateStatus.Check.CheckEvent.NONE -> when (check.status) {
            UpdateStatus.Check.CheckResult.AVAILABLE,
            UpdateStatus.Check.CheckResult.NONE -> if (check.availableVersion.isBlank()) {
                AvailableVersion.Available(check.availableVersion)
            } else {
                AvailableVersion.NotAvailable
            }

            UpdateStatus.Check.CheckResult.NOT_AVAILABLE -> AvailableVersion.NotAvailable
            UpdateStatus.Check.CheckResult.FAILURE -> AvailableVersion.FailedToCheck
        }
    }
}

fun BusyLibUpdateEvent.Update.UpdateCheck.toAvailableVersion(): AvailableVersion {
    return when (event) {
        BusyLibUpdateEvent.Update.UpdateCheck.CheckEvent.START -> AvailableVersion.CheckingOnBBInProgress
        BusyLibUpdateEvent.Update.UpdateCheck.CheckEvent.STOP,
        BusyLibUpdateEvent.Update.UpdateCheck.CheckEvent.NONE -> when (val resultLocal = result) {
            is BusyLibUpdateEvent.Update.UpdateCheck.CheckResult.Available -> {
                AvailableVersion.Available(resultLocal.availableVersion)
            }

            is BusyLibUpdateEvent.Update.UpdateCheck.CheckResult.Unavailable -> {
                when (resultLocal.reason) {
                    BusyLibUpdateEvent.Update.UpdateCheck.CheckResult.Unavailable.CheckError.FAILURE -> {
                        AvailableVersion.FailedToCheck
                    }

                    BusyLibUpdateEvent.Update.UpdateCheck.CheckResult.Unavailable.CheckError.IDLE,
                    BusyLibUpdateEvent.Update.UpdateCheck.CheckResult.Unavailable.CheckError.NOT_AVAILABLE -> {
                        AvailableVersion.NotAvailable
                    }
                }
            }
        }
    }
}