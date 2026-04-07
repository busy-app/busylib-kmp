package net.flipper.bridge.connection.feature.firmwareupdate.util

import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent.Update.UpdateState
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus

internal fun BsbUpdateStatus.merge(event: BusyLibUpdateEvent.Update): BsbUpdateStatus {
    return when (event) {
        is BusyLibUpdateEvent.Update.UpdateCheck -> {
            event.availableVersion
                ?.let { availableVersion ->
                    this.copy(check = this.check.copy(availableVersion = availableVersion))
                } ?: this
        }

        is UpdateState -> {
            this.copy(
                install = this.install.copy(
                    action = event.action,
                    status = event.status
                )
            )
        }

        is BusyLibUpdateEvent.Update.BsbUpdateStatusChanged -> event.bsbUpdateStatus
    }
}
