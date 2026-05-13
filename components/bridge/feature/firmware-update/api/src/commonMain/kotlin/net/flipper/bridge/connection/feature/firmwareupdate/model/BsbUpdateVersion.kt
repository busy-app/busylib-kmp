package net.flipper.bridge.connection.feature.firmwareupdate.model

/**
 * BSB update version for different connection types
 */
sealed interface BsbUpdateVersion {
    data object NoUpdateAvailable : BsbUpdateVersion
    data object FailedToCheck : BsbUpdateVersion
    data object CheckingOnBBInProgress : BsbUpdateVersion

    sealed interface ReadyToUpdate : BsbUpdateVersion {
        val version: String

        /**
         * Default version information for non-lan connections
         */
        data class Default(override val version: String) : ReadyToUpdate

        /**
         * Update version information for lan-oriented connections
         * which should download and extract tgz file manually onto busybar
         */
        data class Url(
            override val version: String,
            val url: String,
            val sha256: String,
            val changelog: String,
            val firmwareChannel: FirmwareChannel
        ) : ReadyToUpdate
    }

    data object Loading : BsbUpdateVersion
}
