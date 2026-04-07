package net.flipper.bridge.connection.feature.firmwareupdate.util

import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent.Update.UpdateState
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus

private fun UpdateState.Action.toRpcAction(): UpdateStatus.Install.Action {
    return when (this) {
        UpdateState.Action.DOWNLOAD -> UpdateStatus.Install.Action.DOWNLOAD
        UpdateState.Action.SHA_VERIFICATION -> UpdateStatus.Install.Action.SHA_VERIFICATION
        UpdateState.Action.UNPACK -> UpdateStatus.Install.Action.UNPACK
        UpdateState.Action.PREPARE -> UpdateStatus.Install.Action.PREPARE
        UpdateState.Action.APPLY -> UpdateStatus.Install.Action.APPLY
        UpdateState.Action.NONE -> UpdateStatus.Install.Action.NONE
    }
}

private fun UpdateState.Status.toRpcStatus(): UpdateStatus.Install.Status {
    return when (this) {
        UpdateState.Status.OK -> UpdateStatus.Install.Status.OK
        UpdateState.Status.BATTERY_LOW -> UpdateStatus.Install.Status.BATTERY_LOW
        UpdateState.Status.BUSY -> UpdateStatus.Install.Status.BUSY
        UpdateState.Status.DOWNLOAD_FAILURE -> UpdateStatus.Install.Status.DOWNLOAD_FAILURE
        UpdateState.Status.DOWNLOAD_ABORT -> UpdateStatus.Install.Status.DOWNLOAD_ABORT
        UpdateState.Status.SHA_MISMATCH -> UpdateStatus.Install.Status.SHA_MISMATCH
        UpdateState.Status.UNPACK_STAGING_DIR_FAILURE -> UpdateStatus.Install.Status.UNPACK_STAGING_DIR_FAILURE
        UpdateState.Status.UNPACK_ARCHIVE_OPEN_FAILURE -> UpdateStatus.Install.Status.UNPACK_ARCHIVE_OPEN_FAILURE
        UpdateState.Status.UNPACK_ARCHIVE_UNPACK_FAILURE -> UpdateStatus.Install.Status.UNPACK_ARCHIVE_UNPACK_FAILURE
        UpdateState.Status.INSTALL_MANIFEST_NOT_FOUND -> UpdateStatus.Install.Status.INSTALL_MANIFEST_NOT_FOUND
        UpdateState.Status.INSTALL_MANIFEST_INVALID -> UpdateStatus.Install.Status.INSTALL_MANIFEST_INVALID
        UpdateState.Status.INSTALL_SESSION_CONFIG_FAILURE -> UpdateStatus.Install.Status.INSTALL_SESSION_CONFIG_FAILURE
        UpdateState.Status.INSTALL_POINTER_SETUP_FAILURE -> UpdateStatus.Install.Status.INSTALL_POINTER_SETUP_FAILURE
        UpdateState.Status.UNKNOWN_FAILURE -> UpdateStatus.Install.Status.UNKNOWN_FAILURE
    }
}

internal fun UpdateStatus.merge(event: BusyLibUpdateEvent.Update): UpdateStatus {
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
                    action = event.action.toRpcAction(),
                    status = event.status.toRpcStatus()
                )
            )
        }

        is BusyLibUpdateEvent.Update.UpdateStatus -> event.status
    }
}
