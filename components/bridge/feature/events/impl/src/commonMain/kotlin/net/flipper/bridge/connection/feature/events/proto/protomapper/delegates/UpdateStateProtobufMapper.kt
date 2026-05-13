package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_Update.UpdateAction
import BSB_Update.UpdateEvent
import BSB_Update.UpdateState
import BSB_Update.UpdateStatus
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent.Update.UpdateState.BsbAction
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent.Update.UpdateState.BsbEvent
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent.Update.UpdateState.BsbStatus

object UpdateStateProtobufMapper {
    fun map(updateState: UpdateState): BusyLibUpdateEvent.Update.UpdateState {
        return BusyLibUpdateEvent.Update.UpdateState(
            event = mapEvent(updateState.event),
            action = mapAction(updateState.action),
            status = mapStatus(updateState.status),
        )
    }

    private fun mapEvent(event: UpdateEvent): BsbEvent {
        return when (event) {
            UpdateEvent.SESSION_START -> BsbEvent.SESSION_START
            UpdateEvent.SESSION_STOP -> BsbEvent.SESSION_STOP
            UpdateEvent.ACTION_BEGIN -> BsbEvent.ACTION_BEGIN
            UpdateEvent.ACTION_DONE -> BsbEvent.ACTION_DONE
            UpdateEvent.DETAIL_CHANGE -> BsbEvent.DETAIL_CHANGE
            UpdateEvent.ACTION_PROGRESS -> BsbEvent.ACTION_PROGRESS
            UpdateEvent.EVENT_NONE,
            is UpdateEvent.Unrecognized -> BsbEvent.NONE
        }
    }

    private fun mapAction(action: UpdateAction): BsbAction {
        return when (action) {
            UpdateAction.DOWNLOAD -> BsbAction.DOWNLOAD
            UpdateAction.SHA_VERIFICATION -> BsbAction.SHA_VERIFICATION
            UpdateAction.UNPACK -> BsbAction.UNPACK
            UpdateAction.INSTALLATION_PREPARE -> BsbAction.PREPARE
            UpdateAction.INSTALLATION_APPLY -> BsbAction.APPLY
            UpdateAction.ACTION_NONE -> BsbAction.NONE
            is UpdateAction.Unrecognized -> BsbAction.NONE
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun mapStatus(status: UpdateStatus): BsbStatus {
        return when (status) {
            UpdateStatus.OK -> BsbStatus.OK
            UpdateStatus.BATTERY_LOW -> BsbStatus.BATTERY_LOW
            UpdateStatus.BUSY -> BsbStatus.BUSY
            UpdateStatus.DOWNLOAD_FAILURE -> BsbStatus.DOWNLOAD_FAILURE
            UpdateStatus.DOWNLOAD_ABORT -> BsbStatus.DOWNLOAD_ABORT
            UpdateStatus.SHA_MISMATCH -> BsbStatus.SHA_MISMATCH
            UpdateStatus.UNPACK_CREATE_STAGING_DIRECTORY_FAILURE -> BsbStatus.UNPACK_STAGING_DIR_FAILURE
            UpdateStatus.UNPACK_ARCHIVE_OPEN_FAILURE -> BsbStatus.UNPACK_ARCHIVE_OPEN_FAILURE
            UpdateStatus.UNPACK_ARCHIVE_UNPACK_FAILURE -> BsbStatus.UNPACK_ARCHIVE_UNPACK_FAILURE
            UpdateStatus.INSTALLATION_PREPARE_MANIFEST_NOT_FOUND -> BsbStatus.INSTALL_MANIFEST_NOT_FOUND
            UpdateStatus.INSTALLATION_PREPARE_MANIFEST_INVALID -> BsbStatus.INSTALL_MANIFEST_INVALID
            UpdateStatus.INSTALLATION_PREPARE_SESSION_CONFIG_SETUP_FAILURE -> BsbStatus.INSTALL_SESSION_CONFIG_FAILURE
            UpdateStatus.INSTALLATION_PREPARE_POINTER_SETUP_FAILURE -> BsbStatus.INSTALL_POINTER_SETUP_FAILURE
            UpdateStatus.UNKNOWN_FAILURE -> BsbStatus.UNKNOWN_FAILURE
            is UpdateStatus.Unrecognized -> BsbStatus.UNKNOWN_FAILURE
        }
    }
}
