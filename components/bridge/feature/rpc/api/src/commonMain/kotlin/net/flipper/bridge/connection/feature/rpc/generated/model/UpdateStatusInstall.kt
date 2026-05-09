package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateStatusInstall(
    @SerialName("is_allowed")
    val isAllowed: kotlin.Boolean,
    @SerialName("event")
    val event: Event,
    @SerialName("action")
    val action: Action,
    @SerialName("status")
    val status: Status,
    @SerialName("detail")
    val detail: kotlin.String,
    @SerialName("download")
    val download: UpdateStatusInstallDownload
) {

    @Serializable
    enum class Event(val value: kotlin.String) {
        @SerialName("session_start")
        SESSION_START("session_start"),

        @SerialName("session_stop")
        SESSION_STOP("session_stop"),

        @SerialName("action_begin")
        ACTION_BEGIN("action_begin"),

        @SerialName("action_done")
        ACTION_DONE("action_done"),

        @SerialName("detail_change")
        DETAIL_CHANGE("detail_change"),

        @SerialName("action_progress")
        ACTION_PROGRESS("action_progress"),

        @SerialName("none")
        NONE("none")
    }

    @Serializable
    enum class Action(val value: kotlin.String) {
        @SerialName("download")
        DOWNLOAD("download"),

        @SerialName("sha_verification")
        SHA_VERIFICATION("sha_verification"),

        @SerialName("unpack")
        UNPACK("unpack"),

        @SerialName("prepare")
        PREPARE("prepare"),

        @SerialName("apply")
        APPLY("apply"),

        @SerialName("none")
        NONE("none")
    }

    @Serializable
    enum class Status(val value: kotlin.String) {
        @SerialName("ok")
        OK("ok"),

        @SerialName("battery_low")
        BATTERY_LOW("battery_low"),

        @SerialName("busy")
        BUSY("busy"),

        @SerialName("download_failure")
        DOWNLOAD_FAILURE("download_failure"),

        @SerialName("download_abort")
        DOWNLOAD_ABORT("download_abort"),

        @SerialName("sha_mismatch")
        SHA_MISMATCH("sha_mismatch"),

        @SerialName("unpack_staging_dir_failure")
        UNPACK_STAGING_DIR_FAILURE("unpack_staging_dir_failure"),

        @SerialName("unpack_archive_open_failure")
        UNPACK_ARCHIVE_OPEN_FAILURE("unpack_archive_open_failure"),

        @SerialName("unpack_archive_unpack_failure")
        UNPACK_ARCHIVE_UNPACK_FAILURE("unpack_archive_unpack_failure"),

        @SerialName("install_manifest_not_found")
        INSTALL_MANIFEST_NOT_FOUND("install_manifest_not_found"),

        @SerialName("install_manifest_invalid")
        INSTALL_MANIFEST_INVALID("install_manifest_invalid"),

        @SerialName("install_session_config_failure")
        INSTALL_SESSION_CONFIG_FAILURE("install_session_config_failure"),

        @SerialName("install_pointer_setup_failure")
        INSTALL_POINTER_SETUP_FAILURE("install_pointer_setup_failure"),

        @SerialName("unknown_failure")
        UNKNOWN_FAILURE("unknown_failure")
    }
}
