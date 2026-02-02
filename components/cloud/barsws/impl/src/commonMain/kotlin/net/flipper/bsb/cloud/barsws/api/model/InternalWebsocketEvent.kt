package net.flipper.bsb.cloud.barsws.api.model

import kotlinx.serialization.Serializable
import net.flipper.bsb.cloud.barsws.api.WebSocketEvent


@Serializable
class InternalWebsocketEvent {

}

fun InternalWebsocketEvent.toPublic() = WebSocketEvent()