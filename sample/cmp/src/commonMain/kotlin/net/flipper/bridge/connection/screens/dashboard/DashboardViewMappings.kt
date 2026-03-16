package net.flipper.bridge.connection.screens.dashboard

import net.flipper.bridge.connection.feature.link.model.LinkedAccountInfo

internal fun LinkedAccountInfo?.toUiText(): String {
    return when (this) {
        null -> "Unavailable"
        LinkedAccountInfo.NotLinked -> "Not linked"
        LinkedAccountInfo.Error -> "Error"
        LinkedAccountInfo.Disconnected -> "Disconnected"
        is LinkedAccountInfo.Linked.SameUser -> "Linked (same user: $linkedMail)"
        is LinkedAccountInfo.Linked.DifferentUser -> "Linked (different user: $linkedMail)"
        is LinkedAccountInfo.Linked.MissingBusyCloud -> "Linked (missing BusyCloud: $linkedMail)"
    }
}

internal fun String?.orUnavailable(): String = this ?: "Unavailable"
