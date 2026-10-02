package net.flipper.tools.oncall.impl.model

import kotlin.uuid.Uuid

internal data class CloudOnCallTarget(
    val uniqueId: String,
    val cloudDeviceId: Uuid
)
