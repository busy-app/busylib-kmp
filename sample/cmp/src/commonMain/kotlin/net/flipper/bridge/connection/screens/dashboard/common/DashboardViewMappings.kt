package net.flipper.bridge.connection.screens.dashboard.common

internal fun <T> T?.orUnavailable(): String = this?.toString() ?: "Unavailable"
