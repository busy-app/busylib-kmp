package net.flipper.bridge.connection.feature.firmwareupdate.model

sealed interface BsbUpdateStatus {
    data class ReadyToInstall(val isAllowed: Boolean) : BsbUpdateStatus
    sealed interface InProgress : BsbUpdateStatus {
        sealed interface Downloading : InProgress {
            data object NotSpecified : Downloading
            data class Specified(
                val speedBytesPerSec: Int,
                val receivedBytes: Int,
                val totalBytes: Int
            ) : Downloading
        }
        data class Other(val stage: ProgressStage) : InProgress {
            enum class ProgressStage {
                SHA_VERIFICATION,
                UNPACK,
                PREPARE,
                APPLY,
            }
        }
    }

    data class FailedUpdate(val reason: Reason) : BsbUpdateStatus {
        enum class Reason {
            BATTERY_LOW,
            DOWNLOAD_FAILURE,
            DOWNLOAD_ABORT,
            SHA_MISMATCH,
            UNPACK_STAGING_DIR_FAILURE,
            UNPACK_ARCHIVE_OPEN_FAILURE,
            UNPACK_ARCHIVE_UNPACK_FAILURE,
            INSTALL_MANIFEST_NOT_FOUND,
            INSTALL_MANIFEST_INVALID,
            INSTALL_SESSION_CONFIG_FAILURE,
            INSTALL_POINTER_SETUP_FAILURE,
            UNKNOWN_FAILURE
        }
    }
}
