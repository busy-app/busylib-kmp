package net.flipper.bridge.connection.feature.firmwareupdate.util

import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.firmwareupdate.model.AvailableVersion
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus

fun UpdateStatus.toAvailableVersion(): AvailableVersion {
    return when (check.status) {
        UpdateStatus.Check.CheckResult.AVAILABLE,
        UpdateStatus.Check.CheckResult.NONE -> when (check.event) {
            UpdateStatus.Check.CheckEvent.START, //TODO return AvailableVersion.CheckingOnBBInProgress
            UpdateStatus.Check.CheckEvent.STOP,
            UpdateStatus.Check.CheckEvent.NONE -> if (check.availableVersion.isBlank()) {
                AvailableVersion.Available(check.availableVersion)
            } else {
                AvailableVersion.NotAvailable
            }
        }
        UpdateStatus.Check.CheckResult.NOT_AVAILABLE -> AvailableVersion.NotAvailable
        UpdateStatus.Check.CheckResult.FAILURE -> AvailableVersion.FailedToCheck
    }
}

fun BusyLibUpdateEvent.Update.UpdateCheck.toAvailableVersion(): AvailableVersion {
    // TODO return AvailableVersion.CheckingOnBBInProgress
    return when (val resultLocal = result) {
        is BusyLibUpdateEvent.Update.UpdateCheck.CheckResult.Available -> AvailableVersion.Available(
            resultLocal.availableVersion
        )

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