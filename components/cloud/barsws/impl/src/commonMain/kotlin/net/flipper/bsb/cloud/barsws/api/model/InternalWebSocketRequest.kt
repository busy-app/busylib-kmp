package net.flipper.bsb.cloud.barsws.api.model

import kotlinx.serialization.Serializable
import net.flipper.bsb.cloud.barsws.api.WebSocketRequest

@Serializable
class InternalWebSocketRequest

fun WebSocketRequest.toInternal() = InternalWebSocketRequest()
