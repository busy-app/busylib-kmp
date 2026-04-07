package net.flipper.bridge.connection.feature.firmwareupdate.util

import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent.Update.UpdateState
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent.Update.UpdateState.Action
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent.Update.UpdateState.Status
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall.BsbAction
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus.BsbInstall.BsbStatus

private fun Action.toBsbAction(): BsbAction {
    return when (this) {
        Action.DOWNLOAD -> BsbAction.DOWNLOAD
        Action.SHA_VERIFICATION -> BsbAction.SHA_VERIFICATION
        Action.UNPACK -> BsbAction.UNPACK
        Action.PREPARE -> BsbAction.PREPARE
        Action.APPLY -> BsbAction.APPLY
        Action.NONE -> BsbAction.NONE
    }
}

private fun Status.toBsbStatus(): BsbStatus {
    return when (this) {
        Status.OK -> BsbStatus.OK
        Status.BATTERY_LOW -> BsbStatus.BATTERY_LOW
        Status.BUSY -> BsbStatus.BUSY
        Status.DOWNLOAD_FAILURE -> BsbStatus.DOWNLOAD_FAILURE
        Status.DOWNLOAD_ABORT -> BsbStatus.DOWNLOAD_ABORT
        Status.SHA_MISMATCH -> BsbStatus.SHA_MISMATCH
        Status.UNPACK_STAGING_DIR_FAILURE -> BsbStatus.UNPACK_STAGING_DIR_FAILURE
        Status.UNPACK_ARCHIVE_OPEN_FAILURE -> BsbStatus.UNPACK_ARCHIVE_OPEN_FAILURE
        Status.UNPACK_ARCHIVE_UNPACK_FAILURE -> BsbStatus.UNPACK_ARCHIVE_UNPACK_FAILURE
        Status.INSTALL_MANIFEST_NOT_FOUND -> BsbStatus.INSTALL_MANIFEST_NOT_FOUND
        Status.INSTALL_MANIFEST_INVALID -> BsbStatus.INSTALL_MANIFEST_INVALID
        Status.INSTALL_SESSION_CONFIG_FAILURE -> BsbStatus.INSTALL_SESSION_CONFIG_FAILURE
        Status.INSTALL_POINTER_SETUP_FAILURE -> BsbStatus.INSTALL_POINTER_SETUP_FAILURE
        Status.UNKNOWN_FAILURE -> BsbStatus.UNKNOWN_FAILURE
    }
}

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
                    action = event.action.toBsbAction(),
                    status = event.status.toBsbStatus()
                )
            )
        }

        is BusyLibUpdateEvent.Update.BsbUpdateStatusChanged -> event.bsbUpdateStatus
    }
}
