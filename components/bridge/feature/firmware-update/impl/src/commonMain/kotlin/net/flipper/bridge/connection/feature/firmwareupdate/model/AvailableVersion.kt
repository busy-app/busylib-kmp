package net.flipper.bridge.connection.feature.firmwareupdate.model

sealed interface AvailableVersion {
    data object NotAvailable : AvailableVersion
    data object Loading : AvailableVersion
    data class Available(val version: String) : AvailableVersion

    data object CheckingOnBBInProgress : AvailableVersion

    data object FailedToCheck : AvailableVersion
}
