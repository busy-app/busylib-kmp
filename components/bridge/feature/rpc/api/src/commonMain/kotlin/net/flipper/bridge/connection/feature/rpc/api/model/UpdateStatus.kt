package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateStatus(
    @SerialName("install")
    val install: Install,
    @SerialName("check")
    val check: Check
) {
    @Serializable
    data class Install(
        @SerialName("is_allowed")
        val isAllowed: Boolean,
        @SerialName("event")
        val event: Event,
        @SerialName("action")
        val action: Action,
        @SerialName("status")
        val status: Status,
        @SerialName("detail")
        val detail: String,
        @SerialName("download")
        val download: Download
    ) {
        @Serializable
        enum class Event {
            @SerialName("session_start")
            SESSION_START,

            @SerialName("session_stop")
            SESSION_STOP,

            @SerialName("action_begin")
            ACTION_BEGIN,

            @SerialName("action_done")
            ACTION_DONE,

            @SerialName("detail_change")
            DETAIL_CHANGE,

            @SerialName("action_progress")
            ACTION_PROGRESS,

            @SerialName("none")
            NONE
        }

        @Serializable
        enum class Action {
            @SerialName("download")
            DOWNLOAD,

            @SerialName("sha_verification")
            SHA_VERIFICATION,

            @SerialName("unpack")
            UNPACK,

            @SerialName("prepare")
            PREPARE,

            @SerialName("apply")
            APPLY,

            @SerialName("none")
            NONE
        }

        @Serializable
        enum class Status {
            @SerialName("ok")
            OK,

            @SerialName("battery_low")
            BATTERY_LOW,

            @SerialName("busy")
            BUSY,

            @SerialName("download_failure")
            DOWNLOAD_FAILURE,

            @SerialName("download_abort")
            DOWNLOAD_ABORT,

            @SerialName("sha_mismatch")
            SHA_MISMATCH,

            @SerialName("unpack_staging_dir_failure")
            UNPACK_STAGING_DIR_FAILURE,

            @SerialName("unpack_archive_open_failure")
            UNPACK_ARCHIVE_OPEN_FAILURE,

            @SerialName("unpack_archive_unpack_failure")
            UNPACK_ARCHIVE_UNPACK_FAILURE,

            @SerialName("install_manifest_not_found")
            INSTALL_MANIFEST_NOT_FOUND,

            @SerialName("install_manifest_invalid")
            INSTALL_MANIFEST_INVALID,

            @SerialName("install_session_config_failure")
            INSTALL_SESSION_CONFIG_FAILURE,

            @SerialName("install_pointer_setup_failure")
            INSTALL_POINTER_SETUP_FAILURE,

            @SerialName("unknown_failure")
            UNKNOWN_FAILURE
        }

        @Serializable
        data class Download(
            @SerialName("speed_bytes_per_sec")
            val speedBytesPerSec: Int,
            @SerialName("received_bytes")
            val receivedBytes: Int,
            @SerialName("total_bytes")
            val totalBytes: Int
        )
    }

    @Serializable
    data class Check(
        @SerialName("available_version")
        val availableVersion: String,
        @SerialName("event")
        val event: CheckEvent,
        @SerialName("status")
        val status: CheckResult
    ) {
        @Serializable
        enum class CheckEvent {
            @SerialName("start")
            START,

            @SerialName("stop")
            STOP,

            @SerialName("none")
            NONE
        }

        @Serializable
        enum class CheckResult {
            @SerialName("available")
            AVAILABLE,

            @SerialName("not_available")
            NOT_AVAILABLE,

            @SerialName("failure")
            FAILURE,

            @SerialName("none")
            NONE
        }
    }
}
