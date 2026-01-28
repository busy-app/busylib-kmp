package net.flipper.bridge.connection.feature.smarthome.model

import net.flipper.bridge.connection.feature.rpc.api.model.MatterCommissioningPayload
import kotlin.time.Duration

data class MatterCommissioningTimeLeftPayload(
    val instance: MatterCommissioningPayload,
    val timeLeft: Duration
)
