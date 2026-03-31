package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_Update.UpdateAction
import BSB_Update.UpdateState
import BSB_Update.UpdateStatus
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent

object UpdateStateProtobufMapper {
    fun map(updateState: UpdateState): BusyLibUpdateEvent.UpdateState {
        return BusyLibUpdateEvent.UpdateState(
            action = mapAction(updateState.action),
            status = mapStatus(updateState.status),
        )
    }

    private fun mapAction(action: UpdateAction): BusyLibUpdateEvent.UpdateState.Action {
        return when (action) {
            UpdateAction.DOWNLOAD -> BusyLibUpdateEvent.UpdateState.Action.DOWNLOAD
            UpdateAction.SHA_VERIFICATION -> BusyLibUpdateEvent.UpdateState.Action.SHA_VERIFICATION
            UpdateAction.UNPACK -> BusyLibUpdateEvent.UpdateState.Action.UNPACK
            UpdateAction.INSTALLATION_PREPARE -> BusyLibUpdateEvent.UpdateState.Action.PREPARE
            UpdateAction.INSTALLATION_APPLY -> BusyLibUpdateEvent.UpdateState.Action.APPLY
            UpdateAction.ACTION_NONE -> BusyLibUpdateEvent.UpdateState.Action.NONE
            is UpdateAction.Unrecognized -> BusyLibUpdateEvent.UpdateState.Action.NONE
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun mapStatus(status: UpdateStatus): BusyLibUpdateEvent.UpdateState.Status {
        return when (status) {
            UpdateStatus.OK -> BusyLibUpdateEvent.UpdateState.Status.OK
            UpdateStatus.BATTERY_LOW -> BusyLibUpdateEvent.UpdateState.Status.BATTERY_LOW
            UpdateStatus.BUSY -> BusyLibUpdateEvent.UpdateState.Status.BUSY
            UpdateStatus.DOWNLOAD_FAILURE -> BusyLibUpdateEvent.UpdateState.Status.DOWNLOAD_FAILURE
            UpdateStatus.DOWNLOAD_ABORT -> BusyLibUpdateEvent.UpdateState.Status.DOWNLOAD_ABORT
            UpdateStatus.SHA_MISMATCH -> BusyLibUpdateEvent.UpdateState.Status.SHA_MISMATCH
            UpdateStatus.UNPACK_CREATE_STAGING_DIRECTORY_FAILURE ->
                BusyLibUpdateEvent.UpdateState.Status.UNPACK_STAGING_DIR_FAILURE
            UpdateStatus.UNPACK_ARCHIVE_OPEN_FAILURE ->
                BusyLibUpdateEvent.UpdateState.Status.UNPACK_ARCHIVE_OPEN_FAILURE
            UpdateStatus.UNPACK_ARCHIVE_UNPACK_FAILURE ->
                BusyLibUpdateEvent.UpdateState.Status.UNPACK_ARCHIVE_UNPACK_FAILURE
            UpdateStatus.INSTALLATION_PREPARE_MANIFEST_NOT_FOUND ->
                BusyLibUpdateEvent.UpdateState.Status.INSTALL_MANIFEST_NOT_FOUND
            UpdateStatus.INSTALLATION_PREPARE_MANIFEST_INVALID ->
                BusyLibUpdateEvent.UpdateState.Status.INSTALL_MANIFEST_INVALID
            UpdateStatus.INSTALLATION_PREPARE_SESSION_CONFIG_SETUP_FAILURE ->
                BusyLibUpdateEvent.UpdateState.Status.INSTALL_SESSION_CONFIG_FAILURE
            UpdateStatus.INSTALLATION_PREPARE_POINTER_SETUP_FAILURE ->
                BusyLibUpdateEvent.UpdateState.Status.INSTALL_POINTER_SETUP_FAILURE
            UpdateStatus.UNKNOWN_FAILURE -> BusyLibUpdateEvent.UpdateState.Status.UNKNOWN_FAILURE
            is UpdateStatus.Unrecognized -> BusyLibUpdateEvent.UpdateState.Status.UNKNOWN_FAILURE
        }
    }
}
