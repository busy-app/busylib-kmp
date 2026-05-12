package net.flipper.bridge.connection.feature.firmwareupdate.model

sealed interface BsbUpdateStatus {
    sealed interface ReadyToInstall : BsbUpdateStatus {
        data object BatteryLow : ReadyToInstall
        data object Ready : ReadyToInstall
    }

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

    data object Loading : BsbUpdateStatus
}
