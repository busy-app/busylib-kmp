package net.flipper.tools.oncall.impl.session

import kotlin.uuid.Uuid

internal sealed interface OnCallSessionRoute {
    data class Lan(val host: String) : OnCallSessionRoute
    data class Cloud(val deviceId: Uuid) : OnCallSessionRoute
}
