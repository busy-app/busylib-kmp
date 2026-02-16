package net.flipper.bsb.cloud.barsws.api

import kotlin.uuid.Uuid

data class WebSocketEvent(
    val barId: Uuid,
    val values: Map<String, Any>
)
