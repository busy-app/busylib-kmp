package net.flipper.bridge.connection.feature.link.model

import kotlin.uuid.Uuid

sealed interface LinkedAccountInfo {
    data object NotLinked : LinkedAccountInfo
    data object Error : LinkedAccountInfo
    data object Disconnected : LinkedAccountInfo

    /**
     * BusyBar is linked to user account
     */
    sealed interface Linked : LinkedAccountInfo {
        /**
         * BusyBar account linked to the same account
         */
        data class SameUser(val linkedMail: Uuid) : Linked

        /**
         * BusyBar is linked to another account
         */
        data class DifferentUser(val linkedMail: Uuid) : Linked

        /**
         * BusyBar is linked to an account, but mobile app is not connected to BusyCloud
         */
        data class MissingBusyCloud(val linkedMail: Uuid) : Linked
    }
}
