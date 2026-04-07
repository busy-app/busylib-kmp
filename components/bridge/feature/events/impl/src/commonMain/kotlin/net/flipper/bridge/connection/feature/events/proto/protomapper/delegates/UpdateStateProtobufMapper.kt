package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_Update.UpdateAction
import BSB_Update.UpdateState
import BSB_Update.UpdateStatus
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent

object UpdateStateProtobufMapper {
    fun map(updateState: UpdateState): BusyLibUpdateEvent.Update.UpdateState {
        return BusyLibUpdateEvent.Update.UpdateState(
            action = mapAction(updateState.action),
            status = mapStatus(updateState.status),
        )
    }

    private fun mapAction(action: UpdateAction): BusyLibUpdateEvent.Update.UpdateState.Action {
        return when (action) {
            UpdateAction.DOWNLOAD -> BusyLibUpdateEvent.Update.UpdateState.Action.DOWNLOAD
            UpdateAction.SHA_VERIFICATION -> BusyLibUpdateEvent.Update.UpdateState.Action.SHA_VERIFICATION
            UpdateAction.UNPACK -> BusyLibUpdateEvent.Update.UpdateState.Action.UNPACK
            UpdateAction.INSTALLATION_PREPARE -> BusyLibUpdateEvent.Update.UpdateState.Action.PREPARE
            UpdateAction.INSTALLATION_APPLY -> BusyLibUpdateEvent.Update.UpdateState.Action.APPLY
            UpdateAction.ACTION_NONE -> BusyLibUpdateEvent.Update.UpdateState.Action.NONE
            is UpdateAction.Unrecognized -> BusyLibUpdateEvent.Update.UpdateState.Action.NONE
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun mapStatus(status: UpdateStatus): BusyLibUpdateEvent.Update.UpdateState.Status {
        return when (status) {
            UpdateStatus.OK -> BusyLibUpdateEvent.Update.UpdateState.Status.OK
            UpdateStatus.BATTERY_LOW -> BusyLibUpdateEvent.Update.UpdateState.Status.BATTERY_LOW
            UpdateStatus.BUSY -> BusyLibUpdateEvent.Update.UpdateState.Status.BUSY
            UpdateStatus.DOWNLOAD_FAILURE -> BusyLibUpdateEvent.Update.UpdateState.Status.DOWNLOAD_FAILURE
            UpdateStatus.DOWNLOAD_ABORT -> BusyLibUpdateEvent.Update.UpdateState.Status.DOWNLOAD_ABORT
            UpdateStatus.SHA_MISMATCH -> BusyLibUpdateEvent.Update.UpdateState.Status.SHA_MISMATCH
            UpdateStatus.UNPACK_CREATE_STAGING_DIRECTORY_FAILURE ->
                BusyLibUpdateEvent.Update.UpdateState.Status.UNPACK_STAGING_DIR_FAILURE
            UpdateStatus.UNPACK_ARCHIVE_OPEN_FAILURE ->
                BusyLibUpdateEvent.Update.UpdateState.Status.UNPACK_ARCHIVE_OPEN_FAILURE
            UpdateStatus.UNPACK_ARCHIVE_UNPACK_FAILURE ->
                BusyLibUpdateEvent.Update.UpdateState.Status.UNPACK_ARCHIVE_UNPACK_FAILURE
            UpdateStatus.INSTALLATION_PREPARE_MANIFEST_NOT_FOUND ->
                BusyLibUpdateEvent.Update.UpdateState.Status.INSTALL_MANIFEST_NOT_FOUND
            UpdateStatus.INSTALLATION_PREPARE_MANIFEST_INVALID ->
                BusyLibUpdateEvent.Update.UpdateState.Status.INSTALL_MANIFEST_INVALID
            UpdateStatus.INSTALLATION_PREPARE_SESSION_CONFIG_SETUP_FAILURE ->
                BusyLibUpdateEvent.Update.UpdateState.Status.INSTALL_SESSION_CONFIG_FAILURE
            UpdateStatus.INSTALLATION_PREPARE_POINTER_SETUP_FAILURE ->
                BusyLibUpdateEvent.Update.UpdateState.Status.INSTALL_POINTER_SETUP_FAILURE
            UpdateStatus.UNKNOWN_FAILURE -> BusyLibUpdateEvent.Update.UpdateState.Status.UNKNOWN_FAILURE
            is UpdateStatus.Unrecognized -> BusyLibUpdateEvent.Update.UpdateState.Status.UNKNOWN_FAILURE
        }
    }
}
