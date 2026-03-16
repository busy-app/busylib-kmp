package net.flipper.bridge.connection.screens.dashboard.common

internal fun String?.orUnavailable(): String = this ?: "Unavailable"
