package net.flipper.bridge.connection.feature.link.model

sealed interface LinkedAccountInfo {
    data object NotLinked : LinkedAccountInfo
    data object Error : LinkedAccountInfo
    data object Disconnected : LinkedAccountInfo
    sealed interface Linked : LinkedAccountInfo {
        data class SameUser(val email: String) : Linked
        data class DifferentUser(val email: String) : Linked
    }
}
