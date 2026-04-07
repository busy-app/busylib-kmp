package net.flipper.bridge.connection.feature.firmwareupdate.model

data class BsbUpdateStatus(
    val install: BsbInstall,
    val check: BsbCheck
) {
    data class BsbInstall(
        val isAllowed: Boolean,
        val action: BsbAction,
        val status: BsbStatus,
        val download: BsbDownload
    ) {
        enum class BsbAction {
            DOWNLOAD,
            SHA_VERIFICATION,
            UNPACK,
            PREPARE,
            APPLY,
            NONE
        }

        enum class BsbStatus {
            OK,
            BATTERY_LOW,
            BUSY,
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

        data class BsbDownload(
            val speedBytesPerSec: Int,
            val receivedBytes: Int,
            val totalBytes: Int
        )
    }

    data class BsbCheck(
        val availableVersion: String,
        val status: BsbCheckResult
    ) {
        enum class BsbCheckResult {
            AVAILABLE,
            NOT_AVAILABLE,
            FAILURE,
            NONE
        }
    }
}
